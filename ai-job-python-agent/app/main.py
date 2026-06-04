from fastapi import FastAPI
from fastapi.responses import FileResponse

from app.api.routes import analyses, auth, jobs, recommendations, resumes
from app.core.config import settings
from app.db.init import init_db


def create_app() -> FastAPI:
    app = FastAPI(title=settings.app_name)
    app.include_router(auth.router, prefix="/api/auth", tags=["auth"])
    app.include_router(resumes.router, prefix="/api/resumes", tags=["resumes"])
    app.include_router(jobs.router, prefix="/api/jobs", tags=["jobs"])
    app.include_router(recommendations.router, prefix="/api", tags=["recommendations"])
    app.include_router(analyses.router, prefix="/api/analyses", tags=["analyses"])

    @app.on_event("startup")
    def on_startup() -> None:
        init_db()

    @app.get("/", include_in_schema=False)
    def root() -> FileResponse:
        return FileResponse("app/static/index.html")

    @app.get("/actuator/health")
    def health() -> dict[str, str]:
        return {"status": "UP"}

    @app.get("/api/agent/info")
    def agent_info() -> dict[str, object]:
        return {
            "provider": f"deepseek:{settings.deepseek_model}",
            "workflow": [
                "ResumeParserAgent",
                "JobAnalyzerAgent",
                "MatchScoringAgent",
                "SuggestionAgent",
                "InterviewAgent",
            ],
            "framework": "LangGraph" if _langgraph_available() else "Sequential fallback",
        }

    return app


app = create_app()


def _langgraph_available() -> bool:
    try:
        import langgraph  # noqa: F401

        return True
    except Exception:
        return False
