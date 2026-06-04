from datetime import datetime
from typing import Any

from pydantic import Field

from app.schemas.base import CamelModel


class CreateAnalysisRequest(CamelModel):
    resume_id: int
    job_id: int


class AnalysisResponse(CamelModel):
    id: int
    resume_id: int
    job_id: int
    status: str
    match_score: int | None = None
    summary: str | None = None
    resume_insight: dict[str, Any] | None = None
    job_insight: dict[str, Any] | None = None
    match_detail: dict[str, Any] | None = None
    suggestions: dict[str, Any] | None = None
    interview_questions: dict[str, Any] | None = None
    agent_trace: list[dict[str, Any]] = Field(default_factory=list)
    error_message: str | None = None
    created_at: datetime
    updated_at: datetime
    completed_at: datetime | None = None
