from datetime import datetime, timezone

from sqlalchemy import String, Integer, DateTime
from sqlalchemy.orm import Mapped, mapped_column
from app.db.session import Base


class Favorite(Base):
    __tablename__ = "favorites"

    user_id: Mapped[str] = mapped_column(String(64), primary_key=True)
    movie_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )


class WatchLater(Base):
    __tablename__ = "watch_later"

    user_id: Mapped[str] = mapped_column(String(64), primary_key=True)
    movie_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )


class Watched(Base):
    __tablename__ = "watched"

    user_id: Mapped[str] = mapped_column(String(64), primary_key=True)
    movie_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    watched_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )