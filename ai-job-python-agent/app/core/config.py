from dataclasses import dataclass
import os


@dataclass(frozen=True)
class Settings:
    app_name: str = "职途雷达 · AI 岗位匹配助手（Python 版）"
    database_url: str = os.getenv("DATABASE_URL", "sqlite:///./ai_job_agent_py.db")
    redis_url: str | None = os.getenv("REDIS_URL")
    jwt_secret_key: str = os.getenv("JWT_SECRET_KEY", "change-me-in-production")
    jwt_expire_hours: int = int(os.getenv("JWT_EXPIRE_HOURS", "24"))
    deepseek_base_url: str = os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com")
    deepseek_model: str = os.getenv("DEEPSEEK_MODEL", "deepseek-chat")
    deepseek_api_key: str | None = os.getenv("DEEPSEEK_API_KEY")


settings = Settings()
