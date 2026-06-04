import re

from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.core.security import create_access_token, hash_password, verify_password
from app.models.user import UserAccount
from app.schemas.auth import AuthRequest, AuthResponse
from app.services.sample_data_service import ensure_user_samples
from app.utils.time import now


class AuthService:
    def __init__(self, db: Session) -> None:
        self.db = db

    def register(self, request: AuthRequest) -> AuthResponse:
        username = self._normalize_username(request.username)
        if self.db.query(UserAccount).filter(UserAccount.username == username).first():
            raise HTTPException(status_code=400, detail="username already exists")

        timestamp = now()
        user = UserAccount(
            username=username,
            password_hash=hash_password(request.password),
            display_name=(request.display_name or "").strip() or None,
            created_at=timestamp,
            updated_at=timestamp,
        )
        self.db.add(user)
        self.db.commit()
        self.db.refresh(user)
        ensure_user_samples(self.db, user)
        return self._response(user)

    def login(self, request: AuthRequest) -> AuthResponse:
        username = self._normalize_username(request.username)
        user = self.db.query(UserAccount).filter(UserAccount.username == username).first()
        if user is None or not verify_password(request.password, user.password_hash):
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="bad username or password")
        return self._response(user)

    def _response(self, user: UserAccount) -> AuthResponse:
        return AuthResponse(
            access_token=create_access_token(user.id, user.username),
            user_id=user.id,
            username=user.username,
            display_name=user.display_name,
        )

    def _normalize_username(self, username: str) -> str:
        value = username.strip().lower()
        if not re.fullmatch(r"[a-z0-9_@.\-]{3,40}", value):
            raise HTTPException(status_code=400, detail="invalid username")
        return value
