from fastapi import APIRouter, Depends, File, Form, UploadFile
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_db
from app.models.user import UserAccount
from app.schemas.resume import ResumeResponse
from app.services.resume_service import ResumeService
from app.utils.file_extractors import extract_resume_file

router = APIRouter()


@router.post("/upload", response_model=ResumeResponse)
async def upload_resume(
    file: UploadFile = File(...),
    title: str | None = Form(default=None),
    owner_name: str | None = Form(default=None, alias="ownerName"),
    target_role: str | None = Form(default=None, alias="targetRole"),
    user: UserAccount = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> ResumeResponse:
    content = await extract_resume_file(file)
    return ResumeService(db).upload(user.id, file.filename, content, title, owner_name, target_role)


@router.get("", response_model=list[ResumeResponse])
def list_resumes(
    user: UserAccount = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[ResumeResponse]:
    return ResumeService(db).list_by_user(user.id)


@router.get("/{resume_id}", response_model=ResumeResponse)
def get_resume(
    resume_id: int,
    user: UserAccount = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> ResumeResponse:
    return ResumeService(db).get_response(user.id, resume_id)


@router.delete("/{resume_id}")
def delete_resume(
    resume_id: int,
    user: UserAccount = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> dict[str, bool]:
    ResumeService(db).delete(user.id, resume_id)
    return {"ok": True}
