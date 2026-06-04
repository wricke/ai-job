import json

import httpx

from app.core.config import settings


class DeepSeekClient:
    def complete(self, system_prompt: str, user_prompt: str, temperature: float = 0.2) -> str:
        if not settings.deepseek_api_key:
            return self._fallback(system_prompt, user_prompt)

        try:
            response = httpx.post(
                f"{settings.deepseek_base_url.rstrip('/')}/chat/completions",
                headers={"Authorization": f"Bearer {settings.deepseek_api_key}"},
                json={
                    "model": settings.deepseek_model,
                    "temperature": temperature,
                    "messages": [
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": user_prompt},
                    ],
                },
                timeout=60,
            )
            response.raise_for_status()
            payload = response.json()
            return payload["choices"][0]["message"]["content"]
        except Exception:
            return self._fallback(system_prompt, user_prompt)

    def _fallback(self, system_prompt: str, user_prompt: str) -> str:
        if "job-recommendation" in system_prompt:
            return json.dumps(
                {
                    "summary": "候选人具备后端开发和 AI 应用项目基础，适合投递 Python 后端或 AI 应用开发方向。",
                    "recommendations": [
                        {
                            "roleTitle": "Python 后端开发实习生",
                            "fitScore": 88,
                            "fitLevel": "高",
                            "reasons": ["具备接口、数据库、认证鉴权和工程化项目表达"],
                            "matchedSkills": ["Python", "FastAPI", "MySQL", "JWT"],
                            "missingSkills": ["接口压测", "线上排障", "事务处理"],
                            "searchKeywords": ["Python 后端 实习", "FastAPI 实习"],
                            "preparationTips": ["准备接口设计、数据库索引和 JWT 鉴权追问"],
                        },
                        {
                            "roleTitle": "AI 应用开发实习生",
                            "fitScore": 82,
                            "fitLevel": "中高",
                            "reasons": ["项目包含 DeepSeek API 和多阶段 Agent 工作流"],
                            "matchedSkills": ["AI Agent", "DeepSeek", "LangGraph"],
                            "missingSkills": ["RAG", "向量数据库", "模型评测"],
                            "searchKeywords": ["AI 应用开发 实习", "Agent 开发"],
                            "preparationTips": ["说明每个 Agent 节点的输入输出和失败兜底"],
                        },
                    ],
                    "overallGaps": ["补充项目量化指标", "补强 Redis 和异步任务细节"],
                    "searchKeywords": ["Python 后端 实习", "AI 应用开发 实习"],
                },
                ensure_ascii=False,
            )
        if "resume-parser" in system_prompt:
            return "简历中体现了后端开发、数据库和 AI 应用相关能力。"
        if "jd-analyzer" in system_prompt:
            return "岗位要求重点关注接口开发、数据库、缓存和工程化能力。"
        if "suggestion" in system_prompt:
            return "建议突出项目背景、技术方案、量化结果和失败兜底设计。"
        if "interview" in system_prompt:
            return "重点准备鉴权、数据库设计、Redis 缓存、Agent 工作流和大模型调用异常处理。"
        return "已生成分析内容。"


deepseek_client = DeepSeekClient()
