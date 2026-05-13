import random
import uuid
from dataclasses import dataclass, field
from typing import Any, Dict, List, Set

from app.services.movie_provider import get_random_movie


@dataclass
class Session:
    id: str
    owner_user_id: str
    friend_user_id: str
    status: str = "INVITED"  # INVITED | ACTIVE | WAITING | FINISHED | REJECTED
    movies: List[Dict[str, Any]] = field(default_factory=list)
    votes_owner: Dict[int, bool] = field(default_factory=dict)
    votes_friend: Dict[int, bool] = field(default_factory=dict)


class FriendSessionService:
    _sessions: Dict[str, Session] = {}

    def invite(self, owner_user_id: str, friend_user_id: str) -> Dict[str, Any]:
        if owner_user_id == friend_user_id:
            raise ValueError("Cannot invite yourself")

        session = Session(
            id=str(uuid.uuid4()),
            owner_user_id=owner_user_id,
            friend_user_id=friend_user_id,
        )
        self._sessions[session.id] = session
        return {
            "session_id": session.id,
            "owner_user_id": session.owner_user_id,
            "friend_user_id": session.friend_user_id,
            "status": session.status,
        }

    def accept(self, session_id: str, user_id: str) -> Dict[str, Any]:
        session = self._get_session(session_id)
        self._ensure_user_in_session(session, user_id)

        if session.status == "REJECTED":
            raise ValueError("Session already rejected")
        if session.status == "FINISHED":
            raise ValueError("Session already finished")

        session.status = "ACTIVE"
        return self._to_dict(session)

    def reject(self, session_id: str, user_id: str) -> Dict[str, Any]:
        session = self._get_session(session_id)
        self._ensure_user_in_session(session, user_id)

        if session.status == "FINISHED":
            raise ValueError("Session already finished")

        session.status = "REJECTED"
        return self._to_dict(session)

    def apply_filters(self, session_id: str, filters: Dict[str, Any]) -> Dict[str, Any]:
        session = self._get_session(session_id)

        if session.status == "REJECTED":
            raise ValueError("Session is rejected")

        movies: List[Dict[str, Any]] = []
        seen_ids: Set[int] = set()

        # Пытаемся собрать до 10 уникальных фильмов по фильтрам
        for _ in range(40):
            if len(movies) >= 10:
                break

            movie = get_random_movie(
                genre=filters.get("genre"),
                country=filters.get("country"),
                year_from=filters.get("year_from"),
                year_to=filters.get("year_to"),
                rating_from=filters.get("rating_from"),
                rating_to=filters.get("rating_to"),
            )

            movie_id = movie.get("id")
            if not movie_id or movie_id in seen_ids:
                continue

            seen_ids.add(movie_id)
            movies.append(
                {
                    "movie_id": movie_id,
                    "title": movie.get("title"),
                    "poster_url": movie.get("poster_url"),
                    "genre": movie.get("genre"),
                    "year": movie.get("year"),
                    "runtime_min": movie.get("runtime_min"),
                    "rating_imdb": movie.get("rating_imdb"),
                }
            )

        # Чтобы не отдавать пустой список при редких фильтрах — хотя бы 1 фильм, если возможно
        if not movies:
            fallback = get_random_movie()
            fallback_id = fallback.get("id") or random.randint(100000, 999999)
            movies.append(
                {
                    "movie_id": fallback_id,
                    "title": fallback.get("title"),
                    "poster_url": fallback.get("poster_url"),
                    "genre": fallback.get("genre"),
                    "year": fallback.get("year"),
                    "runtime_min": fallback.get("runtime_min"),
                    "rating_imdb": fallback.get("rating_imdb"),
                }
            )

        session.movies = movies
        session.votes_owner.clear()
        session.votes_friend.clear()
        session.status = "ACTIVE"

        return self._to_dict(session)

    def vote(self, session_id: str, user_id: str, movie_id: int, liked: bool) -> Dict[str, Any]:
        session = self._get_session(session_id)
        self._ensure_user_in_session(session, user_id)

        if session.status == "REJECTED":
            raise ValueError("Session is rejected")
        if not session.movies:
            raise ValueError("No movies in session. Apply filters first.")

        valid_movie_ids = {m["movie_id"] for m in session.movies if "movie_id" in m}
        if movie_id not in valid_movie_ids:
            raise ValueError("movie_id not found in session movies")

        if user_id == session.owner_user_id:
            session.votes_owner[movie_id] = liked
        else:
            session.votes_friend[movie_id] = liked

        total_movies = len(session.movies)
        owner_done = len(session.votes_owner) >= total_movies
        friend_done = len(session.votes_friend) >= total_movies

        if owner_done and friend_done:
            session.status = "FINISHED"
        else:
            session.status = "WAITING"

        return self._to_dict(session)

    def state(self, session_id: str) -> Dict[str, Any]:
        session = self._get_session(session_id)
        return self._to_dict(session)

    # -------- helpers --------

    def _get_session(self, session_id: str) -> Session:
        if session_id not in self._sessions:
            raise KeyError("Session not found")
        return self._sessions[session_id]

    def _ensure_user_in_session(self, session: Session, user_id: str) -> None:
        if user_id not in (session.owner_user_id, session.friend_user_id):
            raise PermissionError("User is not a participant of this session")

    def _to_dict(self, session: Session) -> Dict[str, Any]:
        return {
            "session_id": session.id,
            "owner_user_id": session.owner_user_id,
            "friend_user_id": session.friend_user_id,
            "status": session.status,
            "movies": session.movies,
            "owner_progress": len(session.votes_owner),
            "friend_progress": len(session.votes_friend),
            "owner_liked_movie_ids": [
                movie_id for movie_id, liked in session.votes_owner.items() if liked
            ],
            "friend_liked_movie_ids": [
                movie_id for movie_id, liked in session.votes_friend.items() if liked
            ],
            "mutual_liked_movie_ids": [
                movie_id
                for movie_id in session.votes_owner.keys()
                if session.votes_owner.get(movie_id) is True
                   and session.votes_friend.get(movie_id) is True
            ],
        }