from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_db
from app.models.user import UserAccount
from app.schemas.job import CreateJobRequest, JobResponse
from app.services.job_service import JobService

router = APIRouter()


@router.post("", response_model=JobResponse)
def create_job(
    request: CreateJobRequest,
    user: UserAccount = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> JobResponse:
    return JobService(db).create(user.id, request)


@router.get("", response_model=list[JobResponse])
def list_jobs(
    user: UserAccount = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[JobResponse]:
    return JobService(db).list_by_user(user.id)
