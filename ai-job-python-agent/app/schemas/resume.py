from datetime import datetime

from app.schemas.base import CamelModel


class ResumeResponse(CamelModel):
    id: int
    title: str
    owner_name: str | None = None
    target_role: str | None = None
    content: str
    created_at: datetime
    updated_at: datetime
