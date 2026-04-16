import requests
from app.core.config import settings


def _headers():
    return {
        "accept": "application/json",
        "X-API-KEY": settings.KINOPOISK_API_KEY
    }


def _normalize_short(raw: dict) -> dict:
    genres = raw.get("genres") or []
    rating = (raw.get("rating") or {}).get("imdb")
    poster = (raw.get("poster") or {}).get("url")
    return {
        "id": raw.get("id"),
        "title": raw.get("name") or raw.get("alternativeName") or "",
        "year": raw.get("year"),
        "poster_url": poster,
        "genre": genres[0].get("name") if genres else None,
        "rating_imdb": rating,
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


def get_random_movie(genre: str | None = None) -> dict:
    url = f"{settings.KINOPOISK_BASE_URL}/movie/random"
    params = [
        ("notNullFields", "name"),
        ("notNullFields", "description"),
        ("notNullFields", "rating.imdb"),
        ("notNullFields", "movieLength"),
        ("notNullFields", "poster.url"),
        ("notNullFields", "year"),
        ("rating.imdb", "6-10"),
    ]
    if genre:
        params.append(("genres.name", genre))

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
    r = requests.get(url, headers=_headers(), params={"query": query, "page": page, "limit": limit}, timeout=15)
    r.raise_for_status()
    data = r.json()
    docs = data.get("docs") or []
    return [_normalize_short(x) for x in docs]