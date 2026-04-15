import uuid
from datetime import datetime, timezone
from sqlalchemy import String, DateTime, ForeignKey
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.db.session import Base

"""
Модель серверной пользовательской сессии.
"""


class Session(Base):
    """
    ORM-модель серверной сессии пользователя.

    Используется для хранения refresh token в хэшированном виде,
    а также метаданных сессии: user agent, IP-адреса и срока действия.
    """

    __tablename__ = "sessions"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    """Идентификатор сессии."""

    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        index=True,
        nullable=False
    )
    """Идентификатор пользователя, которому принадлежит сессия."""

    refresh_token_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    """Хэш refresh token."""

    user_agent: Mapped[str | None] = mapped_column(String(500), nullable=True)
    """User-Agent клиента."""

    ip: Mapped[str | None] = mapped_column(String(64), nullable=True)
    """IP-адрес клиента."""

    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    """Дата и время истечения срока действия сессии."""

    revoked_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    """Дата и время отзыва сессии."""

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc)
    )
    """Дата и время создания сессии."""

    user = relationship("User", back_populates="sessions")
    """Связь с моделью пользователя."""