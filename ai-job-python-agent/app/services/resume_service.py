from pathlib import Path

from fastapi import HTTPException
from sqlalchemy.orm import Session

from app.models.resume import ResumeProfile
from app.schemas.resume import ResumeResponse
from app.utils.time import now


class ResumeService:
    def __init__(self, db: Session) -> None:
        self.db = db

    def upload(
        self,
        user_id: int,
        filename: str | None,
        content: str,
        title: str | None,
        owner_name: str | None,
        target_role: str | None,
    ) -> ResumeResponse:
        timestamp = now()
        resume_title = (title or "").strip() or Path(filename or "resume").stem
        existing = (
            self.db.query(ResumeProfile)
            .filter(ResumeProfile.user_id == user_id, ResumeProfile.content == content)
            .order_by(ResumeProfile.id.desc())
            .first()
        )
        if existing:
            existing.title = resume_title
            existing.owner_name = (owner_name or "").strip() or None
            existing.target_role = (target_role or "").strip() or None
            existing.updated_at = timestamp
            self.db.commit()
            self.db.refresh(existing)
            return self._response(existing)

        resume = ResumeProfile(
            user_id=user_id,
            title=resume_title,
            owner_name=(owner_name or "").strip() or None,
            target_role=(target_role or "").strip() or None,
            content=content,
            created_at=timestamp,
            updated_at=timestamp,
        )
        self.db.add(resume)
        self.db.commit()
        self.db.refresh(resume)
        return self._response(resume)

    def list_by_user(self, user_id: int) -> list[ResumeResponse]:
        resumes = self.db.query(ResumeProfile).filter(ResumeProfile.user_id == user_id).order_by(ResumeProfile.id.desc()).all()
        return [self._response(resume) for resume in resumes]

    def get_entity(self, user_id: int, resume_id: int) -> ResumeProfile:
        resume = self.db.query(ResumeProfile).filter(ResumeProfile.id == resume_id, ResumeProfile.user_id == user_id).first()
        if resume is None:
            raise HTTPException(status_code=404, detail="resume not found")
        return resume

    def get_response(self, user_id: int, resume_id: int) -> ResumeResponse:
        return self._response(self.get_entity(user_id, resume_id))

    def delete(self, user_id: int, resume_id: int) -> None:
        resume = self.get_entity(user_id, resume_id)
        self.db.delete(resume)
        self.db.commit()

    def _response(self, resume: ResumeProfile) -> ResumeResponse:
        return ResumeResponse(
            id=resume.id,
            title=resume.title,
            owner_name=resume.owner_name,
            target_role=resume.target_role,
            content=resume.content,
            created_at=resume.created_at,
            updated_at=resume.updated_at,
        )
