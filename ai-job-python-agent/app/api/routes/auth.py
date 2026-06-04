from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_db
from app.models.user import UserAccount
from app.schemas.auth import AuthRequest, AuthResponse, UserResponse
from app.services.auth_service import AuthService

router = APIRouter()


@router.post("/register", response_model=AuthResponse)
def register(request: AuthRequest, db: Session = Depends(get_db)) -> AuthResponse:
    return AuthService(db).register(request)


@router.post("/login", response_model=AuthResponse)
def login(request: AuthRequest, db: Session = Depends(get_db)) -> AuthResponse:
    return AuthService(db).login(request)


@router.get("/me", response_model=UserResponse)
def me(user: UserAccount = Depends(get_current_user)) -> UserResponse:
    return UserResponse(id=user.id, username=user.username, display_name=user.display_name)


@router.post("/logout")
def logout() -> dict[str, bool]:
    return {"ok": True}
