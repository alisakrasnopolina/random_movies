import uuid
from datetime import datetime, timezone
from sqlalchemy import String, DateTime, ForeignKey, JSON
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.db.session import Base

"""
Модель профиля пользователя.
"""


class Profile(Base):
    """
    ORM-модель профиля пользователя.

    Профиль хранит отображаемое имя, аватар и список любимых жанров.
    Связан с моделью User отношением один-к-одному.
    """

    __tablename__ = "profiles"

    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        primary_key=True
    )
    """Идентификатор пользователя и одновременно первичный ключ профиля."""

    display_name: Mapped[str] = mapped_column(String(100), nullable=False)
    """Отображаемое имя пользователя."""

    avatar_url: Mapped[str | None] = mapped_column(String(500), nullable=True)
    """URL аватара пользователя."""

    favorite_genres: Mapped[list[str] | None] = mapped_column(JSON, nullable=True)
    """Список любимых жанров в формате JSON."""

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc)
    )
    """Дата и время создания профиля."""

    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc)
    )
    """Дата и время последнего обновления профиля."""

    user = relationship("User", back_populates="profile")
    """Связь с моделью пользователя."""