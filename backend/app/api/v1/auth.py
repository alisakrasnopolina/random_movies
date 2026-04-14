from datetime import datetime, timedelta, timezone
from jose import jwt, JWTError
from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy.orm import Session as DBSession

from app.core.config import settings
from app.core.security import (
    hash_password, verify_password,
    create_access_token, create_refresh_token, hash_refresh_token
)
from app.db.session import get_db
from app.models.user import User
from app.models.profile import Profile
from app.models.session import Session as UserSession
from app.schemas.auth import RegisterRequest, LoginRequest, RefreshRequest

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register")
def register(payload: RegisterRequest, db: DBSession = Depends(get_db)):
    existing = db.query(User).filter(User.email == payload.email.lower()).first()
    if existing:
        raise HTTPException(status_code=409, detail="Email already exists")

    user = User(
        email=payload.email.lower(),
        password_hash=hash_password(payload.password),
        is_active=True
    )
    db.add(user)
    db.flush()

    profile = Profile(
        user_id=user.id,
        display_name=payload.display_name,
        avatar_url=None,
        favorite_genres=None
    )
    db.add(profile)
    db.commit()

    return {"message": "registered"}


@router.post("/login")
def login(payload: LoginRequest, request: Request, db: DBSession = Depends(get_db)):
    user = db.query(User).filter(User.email == payload.email.lower()).first()
    if not user or not verify_password(payload.password, user.password_hash):
        raise HTTPException(status_code=401, detail="Invalid credentials")

    session_row = UserSession(
        user_id=user.id,
        refresh_token_hash="tmp",
        user_agent=request.headers.get("user-agent"),
        ip=request.client.host if request.client else None,
        expires_at=datetime.now(timezone.utc) + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS),
    )
    db.add(session_row)
    db.flush()

    refresh_token = create_refresh_token(str(user.id), str(session_row.id))
    session_row.refresh_token_hash = hash_refresh_token(refresh_token)

    access_token = create_access_token(str(user.id))
    db.commit()

    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer",
        "expires_in": settings.ACCESS_TOKEN_EXPIRE_MIN * 60,
        "user": {
            "id": str(user.id),
            "email": user.email,
            "display_name": user.profile.display_name if user.profile else ""
        }
    }


@router.post("/refresh")
def refresh(payload: RefreshRequest, db: DBSession = Depends(get_db)):
    try:
        claims = jwt.decode(payload.refresh_token, settings.JWT_SECRET, algorithms=[settings.JWT_ALG])
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid refresh token")

    if claims.get("type") != "refresh":
        raise HTTPException(status_code=401, detail="Invalid token type")

    session_id = claims.get("sid")
    user_id = claims.get("sub")
    if not session_id or not user_id:
        raise HTTPException(status_code=401, detail="Malformed token")

    session_row = db.query(UserSession).filter(UserSession.id == session_id).first()
    if not session_row:
        raise HTTPException(status_code=401, detail="Session not found")
    if session_row.revoked_at is not None:
        raise HTTPException(status_code=401, detail="Session revoked")
    if session_row.expires_at <= datetime.now(timezone.utc):
        raise HTTPException(status_code=401, detail="Session expired")

    if session_row.refresh_token_hash != hash_refresh_token(payload.refresh_token):
        raise HTTPException(status_code=401, detail="Refresh token mismatch")

    # rotation
    new_refresh = create_refresh_token(user_id, session_id)
    session_row.refresh_token_hash = hash_refresh_token(new_refresh)
    session_row.expires_at = datetime.now(timezone.utc) + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)

    new_access = create_access_token(user_id)
    db.commit()

    return {
        "access_token": new_access,
        "refresh_token": new_refresh,
        "token_type": "bearer",
        "expires_in": settings.ACCESS_TOKEN_EXPIRE_MIN * 60,
    }