from datetime import datetime

from pydantic import Field

from app.schemas.base import CamelModel


class CreateJobRequest(CamelModel):
    company: str | None = Field(default=None, max_length=120)
    title: str = Field(min_length=1, max_length=120)
    source: str | None = Field(default=None, max_length=120)
    description: str = Field(min_length=1)


class JobResponse(CamelModel):
    id: int
    company: str | None = None
    title: str
    source: str | None = None
    description: str
    created_at: datetime
    updated_at: datetime
