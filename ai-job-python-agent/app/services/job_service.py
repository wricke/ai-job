from fastapi import HTTPException
from sqlalchemy.orm import Session

from app.models.job import JobPosting
from app.models.resume import ResumeProfile
from app.schemas.job import CreateJobRequest, JobResponse
from app.schemas.recommendation import RecommendationItem
from app.utils.time import now


class JobService:
    def __init__(self, db: Session) -> None:
        self.db = db

    def create(self, user_id: int, request: CreateJobRequest) -> JobResponse:
        timestamp = now()
        job = JobPosting(
            user_id=user_id,
            company=request.company,
            title=request.title.strip(),
            source=(request.source or "").strip() or None,
            description=request.description.strip(),
            created_at=timestamp,
            updated_at=timestamp,
        )
        self.db.add(job)
        self.db.commit()
        self.db.refresh(job)
        return self._response(job)

    def save_recommendation(self, user_id: int, resume: ResumeProfile, item: RecommendationItem) -> JobPosting:
        source = f"AI 推荐 / 简历 {resume.id}"
        job = (
            self.db.query(JobPosting)
            .filter(JobPosting.user_id == user_id, JobPosting.title == item.role_title, JobPosting.source == source)
            .first()
        )
        timestamp = now()
        if job is None:
            job = JobPosting(user_id=user_id, created_at=timestamp)
            self.db.add(job)
        job.company = "AI 推荐"
        job.title = item.role_title
        job.source = source
        job.description = self._recommendation_description(item)
        job.updated_at = timestamp
        self.db.commit()
        self.db.refresh(job)
        return job

    def list_by_user(self, user_id: int) -> list[JobResponse]:
        jobs = self.db.query(JobPosting).filter(JobPosting.user_id == user_id).order_by(JobPosting.id.desc()).all()
        return [self._response(job) for job in jobs]

    def get_entity(self, user_id: int, job_id: int) -> JobPosting:
        job = self.db.query(JobPosting).filter(JobPosting.id == job_id, JobPosting.user_id == user_id).first()
        if job is None:
            raise HTTPException(status_code=404, detail="job not found")
        return job

    def _response(self, job: JobPosting) -> JobResponse:
        return JobResponse(
            id=job.id,
            company=job.company,
            title=job.title,
            source=job.source,
            description=job.description,
            created_at=job.created_at,
            updated_at=job.updated_at,
        )

    def _recommendation_description(self, item: RecommendationItem) -> str:
        parts = [
            f"Role: {item.role_title}",
            f"Fit score: {item.fit_score} ({item.fit_level})",
            "Reasons:",
            *[f"- {value}" for value in item.reasons],
            "Matched skills:",
            *[f"- {value}" for value in item.matched_skills],
            "Missing skills:",
            *[f"- {value}" for value in item.missing_skills],
            "Search keywords:",
            *[f"- {value}" for value in item.search_keywords],
            "Preparation tips:",
            *[f"- {value}" for value in item.preparation_tips],
        ]
        return "\n".join(parts)
