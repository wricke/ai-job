from datetime import datetime

from pydantic import Field

from app.schemas.base import CamelModel


class RecommendationItem(CamelModel):
    role_title: str
    fit_score: int
    fit_level: str
    reasons: list[str] = Field(default_factory=list)
    matched_skills: list[str] = Field(default_factory=list)
    missing_skills: list[str] = Field(default_factory=list)
    search_keywords: list[str] = Field(default_factory=list)
    preparation_tips: list[str] = Field(default_factory=list)
    job_id: int | None = None


class RecommendationResponse(CamelModel):
    resume_id: int
    resume_title: str
    summary: str
    recommendations: list[RecommendationItem]
    overall_gaps: list[str] = Field(default_factory=list)
    search_keywords: list[str] = Field(default_factory=list)
    generated_at: datetime
