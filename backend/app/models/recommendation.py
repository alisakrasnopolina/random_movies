import uuid
from datetime import datetime, timezone

from sqlalchemy import DateTime, Integer, String, Float, ForeignKey, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.db.session import Base


class Recommendation(Base):
    """
    Рекомендация фильма пользователем.

    Храним snapshot фильма, чтобы лента советов не зависела каждый раз
    от внешнего API фильмов.
    """

    __tablename__ = "recommendations"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4
    )

    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
        index=True
    )

    movie_id: Mapped[int] = mapped_column(Integer, nullable=False, index=True)

    user_rating: Mapped[int] = mapped_column(Integer, nullable=False, default=0)

    movie_title: Mapped[str] = mapped_column(String(300), nullable=False, default="")
    movie_year: Mapped[int | None] = mapped_column(Integer, nullable=True)
    movie_genre: Mapped[str | None] = mapped_column(String(300), nullable=True)
    movie_poster_url: Mapped[str | None] = mapped_column(String(1000), nullable=True)
    movie_runtime_min: Mapped[int | None] = mapped_column(Integer, nullable=True)
    movie_rating_imdb: Mapped[float | None] = mapped_column(Float, nullable=True)

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        nullable=False,
        index=True
    )

    __table_args__ = (
        UniqueConstraint("user_id", "movie_id", name="uq_recommendations_user_movie"),
    )