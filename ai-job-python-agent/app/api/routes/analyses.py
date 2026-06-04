from fastapi import APIRouter, BackgroundTasks, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_db
from app.models.user import UserAccount
from app.schemas.analysis import AnalysisResponse, CreateAnalysisRequest
from app.services.analysis_service import AnalysisService, run_analysis_task

router = APIRouter()


@router.post("", response_model=AnalysisResponse)
def create_analysis(
    request: CreateAnalysisRequest,
    background_tasks: BackgroundTasks,
    user: UserAccount = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> AnalysisResponse:
    task = AnalysisService(db).create(user.id, request)
    background_tasks.add_task(run_analysis_task, task.id)
    return task


@router.get("", response_model=list[AnalysisResponse])
def list_analyses(
    status_filter: str | None = None,
    limit: int = 50,
    user: UserAccount = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[AnalysisResponse]:
    return AnalysisService(db).list_by_user(user.id, status_filter, limit)


@router.get("/{analysis_id}", response_model=AnalysisResponse)
def get_analysis(
    analysis_id: int,
    user: UserAccount = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> AnalysisResponse:
    return AnalysisService(db).get_response(user.id, analysis_id)


@router.post("/{analysis_id}/run", response_model=AnalysisResponse)
def rerun_analysis(
    analysis_id: int,
    background_tasks: BackgroundTasks,
    user: UserAccount = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> AnalysisResponse:
    task = AnalysisService(db).mark_pending(user.id, analysis_id)
    background_tasks.add_task(run_analysis_task, task.id)
    return task
