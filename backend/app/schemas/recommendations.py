from datetime import datetime
from pydantic import BaseModel, Field


class RecommendationCreateRequest(BaseModel):
    movie_id: int = Field(gt=0)
    user_rating: int = Field(ge=1, le=10)

    movie_title: str = ""
    movie_year: int | None = None
    movie_genre: str | None = None
    movie_poster_url: str | None = None
    movie_runtime_min: int | None = None
    movie_rating_imdb: float | None = None


class RecommendationOut(BaseModel):
    id: str

    user_id: str
    user_display_name: str
    user_avatar_url: str | None = None

    movie_id: int
    movie_title: str
    movie_year: int | None = None
    movie_genre: str | None = None
    movie_poster_url: str | None = None
    movie_runtime_min: int | None = None
    movie_rating_imdb: float | None = None

    user_rating: int
    created_at: datetime