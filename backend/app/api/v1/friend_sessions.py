import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy import or_
from sqlalchemy.orm import Session

from app.api.v1.auth import get_current_user
from app.core.security import to_user_error
from app.db.session import get_db
from app.models.friend_session import FriendSession
from app.models.user import User
from app.services.movie_provider import get_random_movies


router = APIRouter(prefix="/friend-sessions", tags=["friend-sessions"])


STATUS_INVITED = "INVITED"
STATUS_ACTIVE = "ACTIVE"
STATUS_WAITING = "WAITING"
STATUS_FINISHED = "FINISHED"
STATUS_REJECTED = "REJECTED"


class InviteRequest(BaseModel):
    friend_user_id: str


class FiltersRequest(BaseModel):
    genre: str | None = None
    country: str | None = None
    year_from: int | None = None
    year_to: int | None = None
    rating_from: float | None = None
    rating_to: float | None = None


class VoteRequest(BaseModel):
    movie_id: int
    liked: bool


def now():
    return datetime.now(timezone.utc)


def to_uuid(value: str, error_code: str):
    try:
        return uuid.UUID(value)
    except Exception:
        to_user_error(error_code, "Invalid UUID", status_code=422)


def get_session_or_error(session_id: str, db: Session) -> FriendSession:
    sid = to_uuid(session_id, "invalid_session_id")
    session = db.query(FriendSession).filter(FriendSession.id == sid).first()

    if not session:
        to_user_error("session_not_found", "Session not found", status_code=404)

    return session


def ensure_participant(session: FriendSession, user: User):
    if session.owner_user_id != user.id and session.friend_user_id != user.id:
        to_user_error("forbidden", "You are not participant of this session", status_code=403)


def vote_progress(votes: dict | None) -> int:
    return len(votes or {})


def add_matched_flags(session: FriendSession) -> list[dict]:
    movies = session.movies or []
    owner_votes = session.owner_votes or {}
    friend_votes = session.friend_votes or {}

    result = []

    for movie in movies:
        item = dict(movie)
        movie_id = str(item.get("movie_id") or item.get("id"))

        owner_liked = owner_votes.get(movie_id) is True
        friend_liked = friend_votes.get(movie_id) is True

        item["matched"] = owner_liked and friend_liked
        result.append(item)

    return result


def serialize_session(session: FriendSession, db: Session):
    owner = db.query(User).filter(User.id == session.owner_user_id).first()
    friend = db.query(User).filter(User.id == session.friend_user_id).first()

    return {
        "session_id": str(session.id),
        "owner_user_id": str(session.owner_user_id),
        "friend_user_id": str(session.friend_user_id),

        "owner_user": user_public_data(owner),
        "friend_user": user_public_data(friend),

        "status": session.status,
        "owner_progress": vote_progress(session.owner_votes),
        "friend_progress": vote_progress(session.friend_votes),

        "owner_votes": session.owner_votes or {},
        "friend_votes": session.friend_votes or {},

        "movies": add_matched_flags(session),
    }


@router.post("/invite")
def invite_friend(
        payload: InviteRequest,
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    friend_id = to_uuid(payload.friend_user_id, "invalid_friend_id")

    if friend_id == current_user.id:
        to_user_error("self_invite", "You cannot invite yourself", status_code=400)

    friend = db.query(User).filter(User.id == friend_id).first()
    if not friend:
        to_user_error("friend_not_found", "Friend not found", status_code=404)

    session = FriendSession(
        owner_user_id=current_user.id,
        friend_user_id=friend_id,
        status=STATUS_INVITED,
        filters=None,
        movies=None,
        owner_votes={},
        friend_votes={},
        updated_at=now(),
    )

    db.add(session)
    db.commit()
    db.refresh(session)

    return serialize_session(session, db)


@router.get("/incoming")
def incoming_invites(
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    sessions = (
        db.query(FriendSession)
        .filter(
            FriendSession.friend_user_id == current_user.id,
            FriendSession.status == STATUS_INVITED,
            )
        .order_by(FriendSession.created_at.desc())
        .limit(5)
        .all()
    )

    return [serialize_session(s, db) for s in sessions]


@router.get("/{session_id}")
def get_session(
        session_id: str,
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    session = get_session_or_error(session_id, db)
    ensure_participant(session, current_user)
    return serialize_session(session, db)


@router.post("/{session_id}/accept")
def accept_invite(
        session_id: str,
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    session = get_session_or_error(session_id, db)

    if session.friend_user_id != current_user.id:
        to_user_error("forbidden", "Only invited friend can accept", status_code=403)

    if session.status != STATUS_INVITED:
        to_user_error("invalid_status", "Invite is not active", status_code=400)

    session.status = STATUS_WAITING
    session.updated_at = now()

    db.commit()
    db.refresh(session)

    return serialize_session(session, db)


@router.post("/{session_id}/reject")
def reject_invite(
        session_id: str,
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    session = get_session_or_error(session_id, db)

    if session.friend_user_id != current_user.id:
        to_user_error("forbidden", "Only invited friend can reject", status_code=403)

    session.status = STATUS_REJECTED
    session.updated_at = now()

    db.commit()
    db.refresh(session)

    return serialize_session(session, db)


@router.post("/{session_id}/filters")
def apply_filters(
        session_id: str,
        payload: FiltersRequest,
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    session = get_session_or_error(session_id, db)
    ensure_participant(session, current_user)

    if session.owner_user_id != current_user.id:
        to_user_error("forbidden", "Only owner can apply filters", status_code=403)

    if session.status != STATUS_WAITING:
        to_user_error("invalid_status", "Friend has not accepted invite yet", status_code=400)

    movies = get_random_movies(
        genre=payload.genre,
        country=payload.country,
        year_from=payload.year_from,
        year_to=payload.year_to,
        rating_from=payload.rating_from,
        rating_to=payload.rating_to,
        limit=10,
    )

    if not movies:
        to_user_error("movies_not_found", "No movies found for these filters", status_code=404)

    session.filters = payload.model_dump()
    session.movies = movies
    session.owner_votes = {}
    session.friend_votes = {}
    session.status = STATUS_ACTIVE
    session.updated_at = now()

    db.commit()
    db.refresh(session)

    return serialize_session(session, db)


@router.post("/{session_id}/vote")
def vote(
        session_id: str,
        payload: VoteRequest,
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    session = get_session_or_error(session_id, db)
    ensure_participant(session, current_user)

    if session.status != STATUS_ACTIVE:
        to_user_error("invalid_status", "Session is not active", status_code=400)

    movies = session.movies or []
    movie_ids = {
        int(m.get("movie_id") or m.get("id"))
        for m in movies
        if m.get("movie_id") or m.get("id")
    }

    if payload.movie_id not in movie_ids:
        to_user_error("invalid_movie", "Movie is not in this session", status_code=400)

    key = str(payload.movie_id)

    if current_user.id == session.owner_user_id:
        votes = dict(session.owner_votes or {})
        votes[key] = payload.liked
        session.owner_votes = votes
    else:
        votes = dict(session.friend_votes or {})
        votes[key] = payload.liked
        session.friend_votes = votes

    total = len(movies)
    owner_done = vote_progress(session.owner_votes) >= total
    friend_done = vote_progress(session.friend_votes) >= total

    if total > 0 and owner_done and friend_done:
        session.status = STATUS_FINISHED

    session.updated_at = now()

    db.commit()
    db.refresh(session)

    return serialize_session(session, db)


@router.get("/me/finished-count/value")
def my_finished_sessions_count(
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    count = (
        db.query(FriendSession)
        .filter(
            or_(
                FriendSession.owner_user_id == current_user.id,
                FriendSession.friend_user_id == current_user.id,
                ),
            FriendSession.status == STATUS_FINISHED,
            )
        .count()
    )

    return {"count": count}

def user_public_data(user: User | None) -> dict:
    if user is None:
        return {
            "id": None,
            "display_name": "",
            "avatar_url": None,
        }

    return {
        "id": str(user.id),
        "display_name": user.profile.display_name if user.profile and user.profile.display_name else user.email,
        "avatar_url": user.profile.avatar_url if user.profile else None,
    }