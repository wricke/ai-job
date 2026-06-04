from typing import Any
import json

from sqlalchemy.orm import Session

from app.ai.deepseek_client import deepseek_client
from app.agents.text_analyzer import text_analyzer
from app.schemas.recommendation import RecommendationItem, RecommendationResponse
from app.services.job_service import JobService
from app.services.resume_service import ResumeService
from app.utils.time import now


class RecommendationService:
    def __init__(self, db: Session) -> None:
        self.db = db

    def recommend(self, user_id: int, resume_id: int) -> RecommendationResponse:
        resume = ResumeService(self.db).get_entity(user_id, resume_id)
        report = self._generate(resume.id, resume.title, resume.target_role, resume.content)
        saved_items: list[RecommendationItem] = []
        job_service = JobService(self.db)
        for item in report.recommendations:
            job = job_service.save_recommendation(user_id, resume, item)
            saved_items.append(item.copy(update={"job_id": job.id}))
        return report.copy(update={"recommendations": saved_items})

    def _generate(self, resume_id: int, title: str, target_role: str | None, content: str) -> RecommendationResponse:
        skills = text_analyzer.find_skills(content)
        projects = text_analyzer.extract_project_signals(content)
        raw = deepseek_client.complete(
            "job-recommendation: recommend internship roles. Return JSON only.",
            f"""
            Resume title: {title}
            Target role: {target_role or "-"}
            Extracted skills: {skills}
            Project signals: {projects}

            Return JSON fields: summary, recommendations, overallGaps, searchKeywords.
            Each recommendation has roleTitle, fitScore, fitLevel, reasons,
            matchedSkills, missingSkills, searchKeywords, preparationTips.

            Resume:
            {content[:6000]}
            """,
        )
        try:
            payload = self._extract_json(raw)
            items = [self._parse_item(item) for item in payload.get("recommendations", []) if isinstance(item, dict)]
            if not items:
                raise ValueError("empty recommendation list")
        except Exception:
            payload = self._extract_json(deepseek_client._fallback("job-recommendation", content))
            items = [self._parse_item(item) for item in payload["recommendations"]]

        return RecommendationResponse(
            resume_id=resume_id,
            resume_title=title,
            summary=str(payload.get("summary") or "已根据简历生成岗位推荐。"),
            recommendations=items[:6],
            overall_gaps=self._clean_strings(payload.get("overallGaps") or payload.get("overall_gaps"), 6),
            search_keywords=self._clean_strings(payload.get("searchKeywords") or payload.get("search_keywords"), 10),
            generated_at=now(),
        )

    def _extract_json(self, text: str) -> dict[str, Any]:
        start = text.find("{")
        end = text.rfind("}")
        if start < 0 or end <= start:
            raise ValueError("AI response is not JSON")
        return json.loads(text[start : end + 1])

    def _parse_item(self, item: dict[str, Any]) -> RecommendationItem:
        score = int(item.get("fitScore") or item.get("fit_score") or 0)
        score = max(0, min(100, score))
        level = str(item.get("fitLevel") or item.get("fit_level") or ("高" if score >= 85 else "中" if score >= 70 else "挑战"))
        return RecommendationItem(
            role_title=str(item.get("roleTitle") or item.get("role_title") or "Python 后端开发实习生").strip(),
            fit_score=score,
            fit_level=level,
            reasons=self._clean_strings(item.get("reasons"), 5),
            matched_skills=self._clean_strings(item.get("matchedSkills") or item.get("matched_skills"), 8),
            missing_skills=self._clean_strings(item.get("missingSkills") or item.get("missing_skills"), 8),
            search_keywords=self._clean_strings(item.get("searchKeywords") or item.get("search_keywords"), 8),
            preparation_tips=self._clean_strings(item.get("preparationTips") or item.get("preparation_tips"), 5),
        )

    def _clean_strings(self, values: Any, limit: int) -> list[str]:
        if not isinstance(values, list):
            return []
        result: list[str] = []
        for value in values:
            if isinstance(value, str) and value.strip() and value.strip() not in result:
                result.append(value.strip())
            if len(result) >= limit:
                break
        return result
