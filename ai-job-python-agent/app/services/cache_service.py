import json
from typing import Any

from sqlalchemy.orm import Session

from app.core.config import settings
from app.models.analysis import AnalysisCache
from app.utils.time import now


class AnalysisCacheService:
    def __init__(self, db: Session) -> None:
        self.db = db

    def find(self, cache_key: str) -> dict[str, Any] | None:
        redis_payload = self._redis_get(cache_key)
        if redis_payload:
            return redis_payload

        cached = self.db.get(AnalysisCache, cache_key)
        if cached is None:
            return None
        return {
            "match_score": cached.match_score,
            "summary": cached.summary,
            "resume_insight": cached.resume_insight,
            "job_insight": cached.job_insight,
            "match_detail": cached.match_detail,
            "suggestions": cached.suggestions,
            "interview_questions": cached.interview_questions,
            "agent_trace": cached.agent_trace,
        }

    def save(self, cache_key: str, report: dict[str, Any]) -> None:
        payload = {
            "match_score": report["match_detail"]["score"],
            "summary": report["summary"],
            "resume_insight": json.dumps(report["resume_insight"], ensure_ascii=False),
            "job_insight": json.dumps(report["job_insight"], ensure_ascii=False),
            "match_detail": json.dumps(report["match_detail"], ensure_ascii=False),
            "suggestions": json.dumps(report["suggestions"], ensure_ascii=False),
            "interview_questions": json.dumps(report["interview_questions"], ensure_ascii=False),
            "agent_trace": json.dumps(report["agent_trace"], ensure_ascii=False),
        }
        self._redis_set(cache_key, payload)

        timestamp = now()
        cached = self.db.get(AnalysisCache, cache_key)
        if cached is None:
            cached = AnalysisCache(cache_key=cache_key, created_at=timestamp)
            self.db.add(cached)
        cached.match_score = payload["match_score"]
        cached.summary = payload["summary"]
        cached.resume_insight = payload["resume_insight"]
        cached.job_insight = payload["job_insight"]
        cached.match_detail = payload["match_detail"]
        cached.suggestions = payload["suggestions"]
        cached.interview_questions = payload["interview_questions"]
        cached.agent_trace = payload["agent_trace"]
        cached.updated_at = timestamp
        self.db.commit()

    def _redis_get(self, cache_key: str) -> dict[str, Any] | None:
        client = self._redis_client()
        if client is None:
            return None
        value = client.get(f"ai-job-analysis:{cache_key}")
        return json.loads(value) if value else None

    def _redis_set(self, cache_key: str, payload: dict[str, Any]) -> None:
        client = self._redis_client()
        if client is not None:
            client.setex(f"ai-job-analysis:{cache_key}", 86400, json.dumps(payload, ensure_ascii=False))

    def _redis_client(self):
        if not settings.redis_url:
            return None
        try:
            import redis

            return redis.from_url(settings.redis_url, decode_responses=True)
        except Exception:
            return None
