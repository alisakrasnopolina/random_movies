from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import JWTError
from sqlalchemy.orm import Session

from app.core.security import decode_token
from app.db.session import get_db
from app.models.user_movie_lists import Favorite, WatchLater, Watched
from app.schemas.user_movies import MovieIdPayload

router = APIRouter(prefix="/users/me", tags=["users-me-movies"])
bearer = HTTPBearer(auto_error=True)


def get_current_user_id(
        credentials: HTTPAuthorizationCredentials = Depends(bearer),
) -> str:
    token = credentials.credentials
    try:
        claims = decode_token(token)
    except JWTError:
        raise HTTPException(status_code=401, detail={"error": "invalid_token", "message": "Invalid access token"})

    if claims.get("type") != "access":
        raise HTTPException(status_code=401, detail={"error": "invalid_token_type", "message": "Access token required"})

    user_id = claims.get("sub")
    if not user_id:
        raise HTTPException(status_code=401, detail={"error": "invalid_token_payload", "message": "Token has no sub"})
    return str(user_id)


@router.post("/favorites")
def add_favorite(
        payload: MovieIdPayload,
        db: Session = Depends(get_db),
        user_id: str = Depends(get_current_user_id),
):
    row = db.query(Favorite).filter(
        Favorite.user_id == user_id,
        Favorite.movie_id == payload.movie_id
    ).first()
    if not row:
        row = Favorite(user_id=user_id, movie_id=payload.movie_id)
        db.add(row)
        db.commit()
    return {"message": "favorite_added", "movie_id": payload.movie_id}


@router.delete("/favorites/{movie_id}")
def remove_favorite(
        movie_id: int,
        db: Session = Depends(get_db),
        user_id: str = Depends(get_current_user_id),
):
    row = db.query(Favorite).filter(
        Favorite.user_id == user_id,
        Favorite.movie_id == movie_id
    ).first()
    if row:
        db.delete(row)
        db.commit()
    return {"message": "favorite_removed", "movie_id": movie_id}


@router.post("/watch-later")
def add_watch_later(
        payload: MovieIdPayload,
        db: Session = Depends(get_db),
        user_id: str = Depends(get_current_user_id),
):
    row = db.query(WatchLater).filter(
        WatchLater.user_id == user_id,
        WatchLater.movie_id == payload.movie_id
    ).first()
    if not row:
        row = WatchLater(user_id=user_id, movie_id=payload.movie_id)
        db.add(row)
        db.commit()
    return {"message": "watch_later_added", "movie_id": payload.movie_id}


@router.delete("/watch-later/{movie_id}")
def remove_watch_later(
        movie_id: int,
        db: Session = Depends(get_db),
        user_id: str = Depends(get_current_user_id),
):
    row = db.query(WatchLater).filter(
        WatchLater.user_id == user_id,
        WatchLater.movie_id == movie_id
    ).first()
    if row:
        db.delete(row)
        db.commit()
    return {"message": "watch_later_removed", "movie_id": movie_id}


@router.post("/watched")
def add_watched(
        payload: MovieIdPayload,
        db: Session = Depends(get_db),
        user_id: str = Depends(get_current_user_id),
):
    row = db.query(Watched).filter(
        Watched.user_id == user_id,
        Watched.movie_id == payload.movie_id
    ).first()
    if not row:
        row = Watched(
            user_id=user_id,
            movie_id=payload.movie_id,
            watched_at=datetime.now(timezone.utc)
        )
        db.add(row)
        db.commit()
    return {"message": "watched_added", "movie_id": payload.movie_id}


@router.delete("/watched/{movie_id}")
def remove_watched(
        movie_id: int,
        db: Session = Depends(get_db),
        user_id: str = Depends(get_current_user_id),
):
    row = db.query(Watched).filter(
        Watched.user_id == user_id,
        Watched.movie_id == movie_id
    ).first()
    if row:
        db.delete(row)
        db.commit()
    return {"message": "watched_removed", "movie_id": movie_id}

@router.get("/favorites")
def get_favorites(
        db: Session = Depends(get_db),
        user_id: str = Depends(get_current_user_id),
):
    rows = db.query(Favorite).filter(Favorite.user_id == user_id).all()
    return {"items": [r.movie_id for r in rows]}


@router.get("/watch-later")
def get_watch_later(
        db: Session = Depends(get_db),
        user_id: str = Depends(get_current_user_id),
):
    rows = db.query(WatchLater).filter(WatchLater.user_id == user_id).all()
    return {"items": [r.movie_id for r in rows]}


@router.get("/watched")
def get_watched(
        db: Session = Depends(get_db),
        user_id: str = Depends(get_current_user_id),
):
    rows = db.query(Watched).filter(Watched.user_id == user_id).all()
    return {"items": [r.movie_id for r in rows]}