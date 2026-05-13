from pydantic import BaseModel, EmailStr
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.v1.auth import get_current_user
from app.core.security import hash_password, to_user_error
from app.db.session import get_db
from app.models.profile import Profile
from app.models.user import User

import uuid
from pathlib import Path

from fastapi import UploadFile, File, Request

from app.core.config import settings

from sqlalchemy import or_
from app.models.user_movie_lists import Watched
from app.models.friend_session import FriendSession
from app.models.recommendation import Recommendation


router = APIRouter(prefix="/profile", tags=["profile"])

AVATAR_DIR = Path("static/avatars")
AVATAR_DIR.mkdir(parents=True, exist_ok=True)

class ProfileMeOut(BaseModel):
    id: str
    email: EmailStr
    display_name: str
    avatar_url: str | None = None
    watched_movies_count: int = 0
    friend_sessions_count: int = 0

class ProfileStatsOut(BaseModel):
    watched_count: int = 0
    finished_sessions_count: int = 0
    recommendations_count: int = 0

class ProfileUpdateRequest(BaseModel):
    display_name: str | None = None
    email: EmailStr | None = None
    new_password: str | None = None


@router.get("/me", response_model=ProfileMeOut)
def get_my_profile(
        current_user: User = Depends(get_current_user),
):
    profile = current_user.profile

    return ProfileMeOut(
        id=str(current_user.id),
        email=current_user.email,
        display_name=profile.display_name if profile else "",
        avatar_url=profile.avatar_url if profile else None,
        watched_movies_count=0,
        friend_sessions_count=0,
    )


@router.patch("/me", response_model=ProfileMeOut)
def update_my_profile(
        payload: ProfileUpdateRequest,
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    if payload.email is not None:
        email = payload.email.lower().strip()

        existing = db.query(User).filter(
            User.email == email,
            User.id != current_user.id
        ).first()

        if existing:
            to_user_error("email_exists", "Email already exists", status_code=409)

        current_user.email = email

    if payload.display_name is not None:
        display_name = payload.display_name.strip()

        if not display_name:
            to_user_error("empty_display_name", "Display name cannot be empty", status_code=422)

        if current_user.profile is None:
            current_user.profile = Profile(
                user_id=current_user.id,
                display_name=display_name,
                avatar_url=None,
                favorite_genres=None,
            )
        else:
            current_user.profile.display_name = display_name

    if payload.new_password is not None and payload.new_password.strip():
        current_user.password_hash = hash_password(payload.new_password)

    db.commit()
    db.refresh(current_user)

    profile = current_user.profile

    return ProfileMeOut(
        id=str(current_user.id),
        email=current_user.email,
        display_name=profile.display_name if profile else "",
        avatar_url=profile.avatar_url if profile else None,
        watched_movies_count=0,
        friend_sessions_count=0,
    )

def get_profile_stats_for_user(user_id, db: Session) -> ProfileStatsOut:
    watched_count = (
        db.query(Watched)
        .filter(Watched.user_id == str(user_id))
        .count()
    )

    finished_sessions_count = (
        db.query(FriendSession)
        .filter(
            or_(
                FriendSession.owner_user_id == user_id,
                FriendSession.friend_user_id == user_id,
                ),
            FriendSession.status == "FINISHED",
            )
        .count()
    )

    recommendations_count = (
        db.query(Recommendation)
        .filter(Recommendation.user_id == user_id)
        .count()
    )

    return ProfileStatsOut(
        watched_count=watched_count,
        finished_sessions_count=finished_sessions_count,
        recommendations_count=recommendations_count,
    )


@router.get("/me/stats", response_model=ProfileStatsOut)
def get_my_profile_stats(
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    return get_profile_stats_for_user(current_user.id, db)


@router.get("/{user_id}/stats", response_model=ProfileStatsOut)
def get_public_profile_stats(
        user_id: str,
        db: Session = Depends(get_db),
):
    try:
        uid = uuid.UUID(user_id)
    except Exception:
        to_user_error("invalid_user_id", "Invalid user id", status_code=422)

    user = db.query(User).filter(User.id == uid).first()
    if not user:
        to_user_error("user_not_found", "User not found", status_code=404)

    return get_profile_stats_for_user(uid, db)

@router.post("/me/avatar")
def upload_my_avatar(
        request: Request,
        file: UploadFile = File(...),
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    # if file.content_type not in ["image/jpeg", "image/png", "image/webp"]:
    #     to_user_error("invalid_avatar_type", "Avatar must be JPG, PNG or WEBP", status_code=422)
    content_type = file.content_type or ""

    if not content_type.startswith("image/"):
        to_user_error("invalid_avatar_type", "Avatar must be an image", status_code=422)

    suffix = ".jpg"
    if file.content_type == "image/png":
        suffix = ".png"
    elif file.content_type == "image/webp":
        suffix = ".webp"

    filename = f"{current_user.id}_{uuid.uuid4().hex}{suffix}"
    filepath = AVATAR_DIR / filename

    content = file.file.read()

    max_size = 5 * 1024 * 1024
    if len(content) > max_size:
        to_user_error("avatar_too_large", "Avatar is too large", status_code=422)

    filepath.write_bytes(content)

    avatar_url = settings.PUBLIC_BASE_URL.rstrip("/") + f"/static/avatars/{filename}"

    if current_user.profile is None:
        current_user.profile = Profile(
            user_id=current_user.id,
            display_name=current_user.email,
            avatar_url=avatar_url,
            favorite_genres=None,
        )
    else:
        current_user.profile.avatar_url = avatar_url

    db.commit()
    db.refresh(current_user)

    return {
        "avatar_url": avatar_url
    }