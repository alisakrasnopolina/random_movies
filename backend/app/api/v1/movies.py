from fastapi import APIRouter, Query, HTTPException
from app.schemas.movie import MovieShortOut, MovieDetailOut
from app.services.movie_provider import get_random_movie, get_movie_by_id, search_movies

router = APIRouter(prefix="/movies", tags=["movies"])


@router.get("/random", response_model=MovieShortOut)
def random_movie(genre: str | None = Query(default=None)):
    try:
        return get_random_movie(genre=genre)
    except Exception:
        raise HTTPException(status_code=502, detail={"error": "provider_error", "message": "Movie provider unavailable"})


@router.get("/{movie_id}", response_model=MovieDetailOut)
def movie_by_id(movie_id: int):
    try:
        return get_movie_by_id(movie_id)
    except Exception:
        raise HTTPException(status_code=502, detail={"error": "provider_error", "message": "Movie provider unavailable"})


@router.get("/search", response_model=list[MovieShortOut])
def movies_search(query: str, page: int = 1, limit: int = 10):
    try:
        return search_movies(query=query, page=page, limit=limit)
    except Exception:
        raise HTTPException(status_code=502, detail={"error": "provider_error", "message": "Movie provider unavailable"})