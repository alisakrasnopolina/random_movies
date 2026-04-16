from pydantic import BaseModel
from typing import List, Optional


class MovieShortOut(BaseModel):
    id: int
    title: str
    year: Optional[int] = None
    poster_url: Optional[str] = None
    genre: Optional[str] = None
    rating_imdb: Optional[float] = None
    runtime_min: Optional[int] = None


class MovieDetailOut(BaseModel):
    id: int
    title: str
    year: Optional[int] = None
    poster_url: Optional[str] = None
    genres: List[str] = []
    countries: List[str] = []
    rating_imdb: Optional[float] = None
    runtime_min: Optional[int] = None
    description: Optional[str] = None
    director: Optional[str] = None
    actors: List[str] = []