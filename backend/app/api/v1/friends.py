import uuid

from pydantic import BaseModel, EmailStr
from fastapi import APIRouter, Depends
from sqlalchemy import or_
from sqlalchemy.orm import Session

from app.api.v1.auth import get_current_user
from app.core.security import to_user_error
from app.db.session import get_db
from app.models.friendship import Friendship
from app.models.user import User


router = APIRouter(prefix="/friends", tags=["friends"])


class FriendOut(BaseModel):
    id: str
    email: EmailStr | None = None
    display_name: str
    avatar_url: str | None = None
    is_friend: bool = True


class FriendshipStatusOut(BaseModel):
    user_id: str
    is_friend: bool


def normalize_pair(current_user_id: uuid.UUID, other_user_id: uuid.UUID) -> tuple[uuid.UUID, uuid.UUID]:
    if str(current_user_id) < str(other_user_id):
        return current_user_id, other_user_id
    return other_user_id, current_user_id


def get_display_name(user: User) -> str:
    if user.profile and user.profile.display_name:
        return user.profile.display_name
    return user.email


def get_avatar_url(user: User) -> str | None:
    if user.profile:
        return user.profile.avatar_url
    return None


@router.get("/me", response_model=list[FriendOut])
def get_my_friends(
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    rows = db.query(Friendship).filter(
        or_(
            Friendship.user_id_1 == current_user.id,
            Friendship.user_id_2 == current_user.id,
            )
    ).all()

    friend_ids = []
    for row in rows:
        if row.user_id_1 == current_user.id:
            friend_ids.append(row.user_id_2)
        else:
            friend_ids.append(row.user_id_1)

    if not friend_ids:
        return []

    users = db.query(User).filter(User.id.in_(friend_ids)).all()

    return [
        FriendOut(
            id=str(user.id),
            email=user.email,
            display_name=get_display_name(user),
            avatar_url=get_avatar_url(user),
            is_friend=True,
        )
        for user in users
    ]


@router.get("/{user_id}/friends", response_model=list[FriendOut])
def get_user_friends(
        user_id: str,
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    try:
        target_user_id = uuid.UUID(user_id)
    except ValueError:
        to_user_error("invalid_user_id", "Invalid user id", status_code=422)

    target_user = db.query(User).filter(User.id == target_user_id).first()
    if not target_user:
        to_user_error("user_not_found", "User not found", status_code=404)

    rows = db.query(Friendship).filter(
        or_(
            Friendship.user_id_1 == target_user_id,
            Friendship.user_id_2 == target_user_id,
            )
    ).all()

    friend_ids = []
    for row in rows:
        if row.user_id_1 == target_user_id:
            friend_ids.append(row.user_id_2)
        else:
            friend_ids.append(row.user_id_1)

    if not friend_ids:
        return []

    users = db.query(User).filter(User.id.in_(friend_ids)).all()

    return [
        FriendOut(
            id=str(user.id),
            email=user.email,
            display_name=get_display_name(user),
            avatar_url=get_avatar_url(user),
            is_friend=True,
        )
        for user in users
    ]


@router.get("/{user_id}/status", response_model=FriendshipStatusOut)
def get_friendship_status(
        user_id: str,
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    try:
        other_user_id = uuid.UUID(user_id)
    except ValueError:
        to_user_error("invalid_user_id", "Invalid user id", status_code=422)

    if other_user_id == current_user.id:
        return FriendshipStatusOut(user_id=user_id, is_friend=False)

    user_id_1, user_id_2 = normalize_pair(current_user.id, other_user_id)

    friendship = db.query(Friendship).filter(
        Friendship.user_id_1 == user_id_1,
        Friendship.user_id_2 == user_id_2,
        ).first()

    return FriendshipStatusOut(
        user_id=user_id,
        is_friend=friendship is not None,
    )


@router.post("/{user_id}", response_model=FriendshipStatusOut)
def add_friend(
        user_id: str,
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    try:
        other_user_id = uuid.UUID(user_id)
    except ValueError:
        to_user_error("invalid_user_id", "Invalid user id", status_code=422)

    if other_user_id == current_user.id:
        to_user_error("self_friendship", "You cannot add yourself as friend", status_code=400)

    other_user = db.query(User).filter(User.id == other_user_id).first()
    if not other_user:
        to_user_error("user_not_found", "User not found", status_code=404)

    user_id_1, user_id_2 = normalize_pair(current_user.id, other_user_id)

    friendship = db.query(Friendship).filter(
        Friendship.user_id_1 == user_id_1,
        Friendship.user_id_2 == user_id_2,
        ).first()

    if friendship is None:
        friendship = Friendship(
            user_id_1=user_id_1,
            user_id_2=user_id_2,
        )
        db.add(friendship)
        db.commit()

    return FriendshipStatusOut(
        user_id=user_id,
        is_friend=True,
    )


@router.delete("/{user_id}", response_model=FriendshipStatusOut)
def remove_friend(
        user_id: str,
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db),
):
    try:
        other_user_id = uuid.UUID(user_id)
    except ValueError:
        to_user_error("invalid_user_id", "Invalid user id", status_code=422)

    user_id_1, user_id_2 = normalize_pair(current_user.id, other_user_id)

    friendship = db.query(Friendship).filter(
        Friendship.user_id_1 == user_id_1,
        Friendship.user_id_2 == user_id_2,
        ).first()

    if friendship:
        db.delete(friendship)
        db.commit()

    return FriendshipStatusOut(
        user_id=user_id,
        is_friend=False,
    )