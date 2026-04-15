import uuid
from datetime import datetime, timezone
from sqlalchemy import String, Boolean, DateTime
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.db.session import Base

"""
Модель пользователя системы.
"""


class User(Base):
    """
    ORM-модель пользователя.

    Хранит основные учетные данные пользователя:
    email, username, хэш пароля и признак активности.
    Также содержит связи с профилем и серверными сессиями.
    """

    __tablename__ = "users"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    """Идентификатор пользователя."""

    email: Mapped[str] = mapped_column(String(320), unique=True, index=True, nullable=False)
    """Email пользователя."""

    username: Mapped[str | None] = mapped_column(String(50), unique=True, nullable=True)
    """Уникальный username пользователя."""

    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    """Хэш пароля пользователя."""

    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    """Признак активности учетной записи."""

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc)
    )
    """Дата и время создания пользователя."""

    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc)
    )
    """Дата и время последнего обновления пользователя."""

    profile = relationship("Profile", back_populates="user", uselist=False, cascade="all, delete-orphan")
    """Связь один-к-одному с профилем пользователя."""

    sessions = relationship("Session", back_populates="user", cascade="all, delete-orphan")
    """Связь один-ко-многим с серверными сессиями пользователя."""