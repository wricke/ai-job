from pydantic import Field

from app.schemas.base import CamelModel


class AuthRequest(CamelModel):
    username: str = Field(min_length=3, max_length=40)
    password: str = Field(min_length=6, max_length=120)
    display_name: str | None = Field(default=None, max_length=80)


class AuthResponse(CamelModel):
    authenticated: bool = True
    access_token: str
    token_type: str = "bearer"
    user_id: int
    username: str
    display_name: str | None = None


class UserResponse(CamelModel):
    authenticated: bool = True
    id: int
    username: str
    display_name: str | None = None
