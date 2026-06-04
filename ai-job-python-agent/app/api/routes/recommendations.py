from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_db
from app.models.user import UserAccount
from app.schemas.recommendation import RecommendationResponse
from app.services.recommendation_service import RecommendationService

router = APIRouter()


@router.post("/resumes/{resume_id}/job-recommendations", response_model=RecommendationResponse)
def recommend_jobs(
    resume_id: int,
    user: UserAccount = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> RecommendationResponse:
    return RecommendationService(db).recommend(user.id, resume_id)
