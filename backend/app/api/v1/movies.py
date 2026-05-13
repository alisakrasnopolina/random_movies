import logging

from fastapi import APIRouter, Query, HTTPException
from requests import HTTPError, Timeout, RequestException

from app.schemas.movie import MovieShortOut, MovieDetailOut
from app.services.movie_provider import get_random_movie, get_movie_by_id, search_movies

router = APIRouter(prefix="/movies", tags=["movies"])
logger = logging.getLogger("movies")


def provider_error(exc: Exception, context: str):
    if isinstance(exc, HTTPError) and exc.response is not None:
        status = exc.response.status_code
        body = exc.response.text[:500]

        logger.error(
            "kinopoisk_http_error context=%s status=%s body=%s",
            context,
            status,
            body,
        )

        raise HTTPException(
            status_code=502,
            detail={
                "error": "provider_error",
                "message": f"Movie provider HTTP {status}",
                "provider_body": body,
            },
        )

    if isinstance(exc, Timeout):
        logger.error("kinopoisk_timeout context=%s", context)
        raise HTTPException(
            status_code=502,
            detail={
                "error": "provider_timeout",
                "message": "Movie provider timeout",
            },
        )

    if isinstance(exc, RequestException):
        logger.error("kinopoisk_request_error context=%s error=%s", context, str(exc))
        raise HTTPException(
            status_code=502,
            detail={
                "error": "provider_error",
                "message": str(exc),
            },
        )

    logger.exception("movie_provider_unexpected_error context=%s", context)
    raise HTTPException(
        status_code=502,
        detail={
            "error": "provider_error",
            "message": f"{type(exc).__name__}: {exc}",
        },
    )


@router.get("/random", response_model=MovieShortOut)
def random_movie(
        genre: str | None = Query(default=None),
        country: str | None = Query(default=None),
        year_from: int | None = Query(default=None),
        year_to: int | None = Query(default=None),
        rating_from: float | None = Query(default=None),
        rating_to: float | None = Query(default=None),
):
    try:
        return get_random_movie(
            genre=genre,
            country=country,
            year_from=year_from,
            year_to=year_to,
            rating_from=rating_from,
            rating_to=rating_to,
        )
    except Exception as e:
        provider_error(e, "random_movie")


@router.get("/search", response_model=list[MovieShortOut])
def movies_search(query: str, page: int = 1, limit: int = 10):
    try:
        return search_movies(query=query, page=page, limit=limit)
    except Exception as e:
        provider_error(e, f"search query={query}")


@router.get("/{movie_id}", response_model=MovieDetailOut)
def movie_by_id(movie_id: int):
    try:
        return get_movie_by_id(movie_id)
    except Exception as e:
        provider_error(e, f"movie_by_id movie_id={movie_id}")