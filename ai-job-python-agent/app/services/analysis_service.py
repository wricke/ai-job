import hashlib
import json

from fastapi import HTTPException
from sqlalchemy.orm import Session

from app.agents.workflow import agent_workflow
from app.db.session import SessionLocal
from app.models.analysis import AnalysisTask
from app.models.job import JobPosting
from app.models.resume import ResumeProfile
from app.schemas.analysis import AnalysisResponse, CreateAnalysisRequest
from app.services.cache_service import AnalysisCacheService
from app.services.job_service import JobService
from app.services.resume_service import ResumeService
from app.utils.json import dump_json, load_json_object
from app.utils.time import now


class AnalysisService:
    def __init__(self, db: Session) -> None:
        self.db = db

    def create(self, user_id: int, request: CreateAnalysisRequest) -> AnalysisResponse:
        resume = ResumeService(self.db).get_entity(user_id, request.resume_id)
        job = JobService(self.db).get_entity(user_id, request.job_id)
        timestamp = now()
        task = AnalysisTask(
            user_id=user_id,
            resume_id=resume.id,
            job_id=job.id,
            status="PENDING",
            cache_key=self._cache_key(user_id, resume, job),
            created_at=timestamp,
            updated_at=timestamp,
        )
        self.db.add(task)
        self.db.commit()
        self.db.refresh(task)
        return self._response(task)

    def list_by_user(self, user_id: int, status_filter: str | None, limit: int) -> list[AnalysisResponse]:
        query = self.db.query(AnalysisTask).filter(AnalysisTask.user_id == user_id)
        if status_filter:
            query = query.filter(AnalysisTask.status == status_filter.upper())
        tasks = query.order_by(AnalysisTask.id.desc()).limit(max(1, min(limit, 100))).all()
        return [self._response(task) for task in tasks]

    def get_entity(self, user_id: int, analysis_id: int) -> AnalysisTask:
        task = self.db.query(AnalysisTask).filter(AnalysisTask.id == analysis_id, AnalysisTask.user_id == user_id).first()
        if task is None:
            raise HTTPException(status_code=404, detail="analysis task not found")
        return task

    def get_response(self, user_id: int, analysis_id: int) -> AnalysisResponse:
        return self._response(self.get_entity(user_id, analysis_id))

    def mark_pending(self, user_id: int, analysis_id: int) -> AnalysisResponse:
        task = self.get_entity(user_id, analysis_id)
        task.status = "PENDING"
        task.error_message = None
        task.updated_at = now()
        self.db.commit()
        self.db.refresh(task)
        return self._response(task)

    def _cache_key(self, user_id: int, resume: ResumeProfile, job: JobPosting) -> str:
        raw = f"{user_id}\n---RESUME---\n{resume.content}\n---JOB---\n{job.description}"
        return hashlib.sha256(raw.encode()).hexdigest()

    def _response(self, task: AnalysisTask) -> AnalysisResponse:
        return AnalysisResponse(
            id=task.id,
            resume_id=task.resume_id,
            job_id=task.job_id,
            status=task.status,
            match_score=task.match_score,
            summary=task.summary,
            resume_insight=load_json_object(task.resume_insight),
            job_insight=load_json_object(task.job_insight),
            match_detail=load_json_object(task.match_detail),
            suggestions=load_json_object(task.suggestions),
            interview_questions=load_json_object(task.interview_questions),
            agent_trace=[] if task.agent_trace is None else json.loads(task.agent_trace),
            error_message=task.error_message,
            created_at=task.created_at,
            updated_at=task.updated_at,
            completed_at=task.completed_at,
        )


def run_analysis_task(task_id: int) -> None:
    db = SessionLocal()
    try:
        task = db.get(AnalysisTask, task_id)
        if task is None:
            return
        task.status = "RUNNING"
        task.updated_at = now()
        db.commit()

        cache_service = AnalysisCacheService(db)
        cached = cache_service.find(task.cache_key)
        if cached:
            _complete_from_cache(db, task, cached)
            return

        resume = db.query(ResumeProfile).filter(ResumeProfile.id == task.resume_id, ResumeProfile.user_id == task.user_id).first()
        job = db.query(JobPosting).filter(JobPosting.id == task.job_id, JobPosting.user_id == task.user_id).first()
        if resume is None or job is None:
            raise RuntimeError("resume or job not found")

        report = agent_workflow.run(resume, job)
        _complete_from_report(db, task, report)
        cache_service.save(task.cache_key, report)
    except Exception as exc:
        task = db.get(AnalysisTask, task_id)
        if task is not None:
            task.status = "FAILED"
            task.error_message = f"{exc.__class__.__name__}: {exc}"
            task.updated_at = now()
            db.commit()
    finally:
        db.close()


def _complete_from_cache(db: Session, task: AnalysisTask, cached: dict) -> None:
    timestamp = now()
    task.status = "COMPLETED"
    task.match_score = int(cached["match_score"])
    task.summary = str(cached["summary"]) + "（命中缓存）"
    task.resume_insight = cached["resume_insight"]
    task.job_insight = cached["job_insight"]
    task.match_detail = cached["match_detail"]
    task.suggestions = cached["suggestions"]
    task.interview_questions = cached["interview_questions"]
    task.agent_trace = cached["agent_trace"]
    task.error_message = None
    task.updated_at = timestamp
    task.completed_at = timestamp
    db.commit()


def _complete_from_report(db: Session, task: AnalysisTask, report: dict) -> None:
    timestamp = now()
    task.status = "COMPLETED"
    task.match_score = int(report["match_detail"]["score"])
    task.summary = report["summary"]
    task.resume_insight = dump_json(report["resume_insight"])
    task.job_insight = dump_json(report["job_insight"])
    task.match_detail = dump_json(report["match_detail"])
    task.suggestions = dump_json(report["suggestions"])
    task.interview_questions = dump_json(report["interview_questions"])
    task.agent_trace = dump_json(report["agent_trace"])
    task.error_message = None
    task.updated_at = timestamp
    task.completed_at = timestamp
    db.commit()
