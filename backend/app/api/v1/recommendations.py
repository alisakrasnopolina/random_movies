from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.v1.auth import get_current_user
from app.core.security import to_user_error
from app.db.session import get_db
from app.models.recommendation import Recommendation
from app.models.user import User
from app.models.profile import Profile
from app.schemas.recommendations import RecommendationCreateRequest, RecommendationOut


router = APIRouter(prefix="/recommendations", tags=["recommendations"])


def display_name_for(user: User) -> str:
    if user.profile and user.profile.display_name:
        return user.profile.display_name
    return user.email


def avatar_for(user: User) -> str | None:
    if user.profile:
        return user.profile.avatar_url
    return None


def to_out(row: Recommendation, user: User) -> RecommendationOut:
    return RecommendationOut(
        id=str(row.id),

        user_id=str(row.user_id),
        user_display_name=display_name_for(user),
        user_avatar_url=avatar_for(user),

        movie_id=row.movie_id,
        movie_title=row.movie_title,
        movie_year=row.movie_year,
        movie_genre=row.movie_genre,
        movie_poster_url=row.movie_poster_url,
        movie_runtime_min=row.movie_runtime_min,
        movie_rating_imdb=row.movie_rating_imdb,

        user_rating=row.user_rating,
        created_at=row.created_at,
    )


@router.post("", response_model=RecommendationOut)
def create_recommendation(
        payload: RecommendationCreateRequest,
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    title = payload.movie_title.strip() if payload.movie_title else ""
    if not title:
        to_user_error("empty_movie_title", "Movie title is required", status_code=422)

    row = db.query(Recommendation).filter(
        Recommendation.user_id == current_user.id,
        Recommendation.movie_id == payload.movie_id,
        ).first()

    if row is None:
        row = Recommendation(
            user_id=current_user.id,
            movie_id=payload.movie_id,
        )
        db.add(row)

    row.user_rating = payload.user_rating
    row.movie_title = title
    row.movie_year = payload.movie_year
    row.movie_genre = payload.movie_genre
    row.movie_poster_url = payload.movie_poster_url
    row.movie_runtime_min = payload.movie_runtime_min
    row.movie_rating_imdb = payload.movie_rating_imdb

    db.commit()
    db.refresh(row)

    return to_out(row, current_user)


@router.get("", response_model=list[RecommendationOut])
def get_recommendations(
        limit: int = 50,
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    limit = max(1, min(limit, 100))

    rows = (
        db.query(Recommendation)
        .filter(Recommendation.user_id != current_user.id)
        .order_by(Recommendation.created_at.desc())
        .limit(limit)
        .all()
    )

    user_ids = [r.user_id for r in rows]
    users = db.query(User).filter(User.id.in_(user_ids)).all() if user_ids else []
    users_by_id = {u.id: u for u in users}

    result = []
    for row in rows:
        user = users_by_id.get(row.user_id)
        if user is None:
            continue
        result.append(to_out(row, user))

    return result

@router.delete("/{movie_id}")
def delete_my_recommendation(
        movie_id: int,
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    row = db.query(Recommendation).filter(
        Recommendation.user_id == current_user.id,
        Recommendation.movie_id == movie_id,
        ).first()

    if row:
        db.delete(row)
        db.commit()

    return {"message": "recommendation_removed", "movie_id": movie_id}