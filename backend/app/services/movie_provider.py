import requests

from app.core.config import settings


def _headers() -> dict:
    return {
        "X-API-KEY": settings.KINOPOISK_API_KEY,
        "accept": "application/json",
    }


def _normalize_short(raw: dict) -> dict:
    genres = [g.get("name") for g in (raw.get("genres") or []) if g.get("name")]
    countries = [c.get("name") for c in (raw.get("countries") or []) if c.get("name")]

    return {
        "id": raw.get("id"),
        "title": raw.get("name") or raw.get("alternativeName") or "",
        "year": raw.get("year"),
        "poster_url": (raw.get("poster") or {}).get("url"),
        "genre": ", ".join(genres[:2]) if genres else "—",
        "country": ", ".join(countries[:3]) if countries else "—",
        "rating_imdb": (raw.get("rating") or {}).get("imdb"),
        "runtime_min": raw.get("movieLength"),
    }


def _normalize_detail(raw: dict) -> dict:
    genres = [g.get("name") for g in (raw.get("genres") or []) if g.get("name")]
    countries = [c.get("name") for c in (raw.get("countries") or []) if c.get("name")]

    director = None
    actors = []
    for p in (raw.get("persons") or []):
        prof = p.get("enProfession")
        name = p.get("name")
        if prof == "director" and name and not director:
            director = name
        if prof == "actor" and name:
            actors.append(name)

    return {
        "id": raw.get("id"),
        "title": raw.get("name") or raw.get("alternativeName") or "",
        "year": raw.get("year"),
        "poster_url": (raw.get("poster") or {}).get("url"),
        "genres": genres,
        "countries": countries,
        "rating_imdb": (raw.get("rating") or {}).get("imdb"),
        "runtime_min": raw.get("movieLength"),
        "description": raw.get("description"),
        "director": director,
        "actors": actors[:10],
    }


def get_random_movie(
        genre: str | None = None,
        country: str | None = None,
        year_from: int | None = None,
        year_to: int | None = None,
        rating_from: float | None = None,
        rating_to: float | None = None,
) -> dict:
    url = f"{settings.KINOPOISK_BASE_URL}/movie/random"
    params = [
        ("notNullFields", "name"),
        ("notNullFields", "description"),
        ("notNullFields", "rating.imdb"),
        ("notNullFields", "movieLength"),
        ("notNullFields", "poster.url"),
        ("notNullFields", "year"),
    ]

    if rating_from is not None or rating_to is not None:
        low = rating_from if rating_from is not None else 0
        high = rating_to if rating_to is not None else 10
        params.append(("rating.imdb", f"{low}-{high}"))
    else:
        params.append(("rating.imdb", "6-10"))

    if genre:
        params.append(("genres.name", genre))
    if country:
        params.append(("countries.name", country))
    if year_from is not None or year_to is not None:
        low = year_from if year_from is not None else 1900
        high = year_to if year_to is not None else 2100
        params.append(("year", f"{low}-{high}"))

    r = requests.get(url, headers=_headers(), params=params, timeout=15)
    r.raise_for_status()
    return _normalize_short(r.json())


def get_movie_by_id(movie_id: int) -> dict:
    url = f"{settings.KINOPOISK_BASE_URL}/movie/{movie_id}"
    r = requests.get(url, headers=_headers(), timeout=15)
    r.raise_for_status()
    return _normalize_detail(r.json())


def search_movies(query: str, page: int = 1, limit: int = 10) -> list[dict]:
    url = f"{settings.KINOPOISK_BASE_URL}/movie/search"
    r = requests.get(
        url,
        headers=_headers(),
        params={"query": query, "page": page, "limit": limit},
        timeout=15,
    )
    r.raise_for_status()
    data = r.json()
    docs = data.get("docs") or []
    return [_normalize_short(x) for x in docs]

# def get_random_movies(
#         genre: str | None = None,
#         country: str | None = None,
#         year_from: int | None = None,
#         year_to: int | None = None,
#         rating_from: float | None = None,
#         rating_to: float | None = None,
#         limit: int = 10,
# ) -> list[dict]:
#     movies: list[dict] = []
#     seen_ids: set[int] = set()
#
#     attempts = 0
#     max_attempts = limit * 5
#
#     while len(movies) < limit and attempts < max_attempts:
#         attempts += 1
#
#         try:
#             movie = get_random_movie(
#                 genre=genre,
#                 country=country,
#                 year_from=year_from,
#                 year_to=year_to,
#                 rating_from=rating_from,
#                 rating_to=rating_to,
#             )
#         except Exception:
#             continue
#
#         movie_id = movie.get("id")
#         if not movie_id:
#             continue
#
#         if movie_id in seen_ids:
#             continue
#
#         seen_ids.add(movie_id)
#
#         movies.append({
#             "movie_id": movie_id,
#             "title": movie.get("title") or "",
#             "poster_url": movie.get("poster_url") or "",
#             "genre": movie.get("genre") or "—",
#             "year": movie.get("year"),
#             "runtime_min": movie.get("runtime_min"),
#             "rating_imdb": movie.get("rating_imdb"),
#             "matched": False,
#         })
#
#     return movies

def get_random_movies(
        genre: str | None = None,
        country: str | None = None,
        year_from: int | None = None,
        year_to: int | None = None,
        rating_from: float | None = None,
        rating_to: float | None = None,
        limit: int = 10,
) -> list[dict]:
    url = f"{settings.KINOPOISK_BASE_URL}/movie"

    params = [
        ("limit", max(limit, 10)),
        ("page", 1),
        ("notNullFields", "name"),
        ("notNullFields", "rating.imdb"),
        ("notNullFields", "movieLength"),
        ("notNullFields", "poster.url"),
        ("notNullFields", "year"),
    ]

    if rating_from is not None or rating_to is not None:
        low = rating_from if rating_from is not None else 0
        high = rating_to if rating_to is not None else 10
        params.append(("rating.imdb", f"{low}-{high}"))
    else:
        params.append(("rating.imdb", "6-10"))

    if genre:
        params.append(("genres.name", genre))

    if country:
        params.append(("countries.name", country))

    if year_from is not None or year_to is not None:
        low = year_from if year_from is not None else 1900
        high = year_to if year_to is not None else 2100
        params.append(("year", f"{low}-{high}"))

    r = requests.get(url, headers=_headers(), params=params, timeout=20)
    r.raise_for_status()

    data = r.json()
    docs = data.get("docs") or []

    movies = []
    seen_ids = set()

    for raw in docs:
        movie = _normalize_short(raw)
        movie_id = movie.get("id")

        if not movie_id or movie_id in seen_ids:
            continue

        seen_ids.add(movie_id)

        movies.append({
            "movie_id": movie_id,
            "title": movie.get("title") or "",
            "poster_url": movie.get("poster_url") or "",
            "genre": movie.get("genre") or "—",
            "year": movie.get("year"),
            "runtime_min": movie.get("runtime_min"),
            "rating_imdb": movie.get("rating_imdb"),
            "matched": False,
        })

        if len(movies) >= limit:
            break

    return movies