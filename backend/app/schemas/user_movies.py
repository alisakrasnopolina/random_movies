from pydantic import BaseModel, Field


class MovieIdPayload(BaseModel):
    movie_id: int = Field(gt=0)