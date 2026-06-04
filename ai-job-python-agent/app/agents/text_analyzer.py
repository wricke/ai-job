import re


class TextAnalyzer:
    skill_aliases: dict[str, list[str]] = {
        "Python": ["python", "fastapi", "django", "flask"],
        "FastAPI": ["fastapi"],
        "SQLAlchemy": ["sqlalchemy"],
        "MySQL": ["mysql", "sql", "数据库"],
        "Redis": ["redis", "缓存"],
        "Celery": ["celery"],
        "Docker": ["docker", "容器"],
        "Git": ["git"],
        "RESTful API": ["restful", "rest api", "接口"],
        "JWT": ["jwt", "token"],
        "LangChain": ["langchain"],
        "LangGraph": ["langgraph"],
        "AI Agent": ["agent", "智能体", "大模型", "llm", "deepseek", "openai"],
    }

    def find_skills(self, text: str) -> list[str]:
        normalized = f" {text or ''} ".lower()
        return [
            skill
            for skill, aliases in self.skill_aliases.items()
            if any(alias.lower() in normalized for alias in aliases)
        ]

    def extract_project_signals(self, text: str, limit: int = 6) -> list[str]:
        return self._pick_sentences(text, ["项目", "系统", "平台", "负责", "实现", "优化", "缓存", "异步"], limit)

    def extract_responsibilities(self, text: str, limit: int = 6) -> list[str]:
        return self._pick_sentences(text, ["岗位职责", "职责", "负责", "参与", "开发", "优化", "设计"], limit)

    def extract_bonus_items(self, text: str, limit: int = 5) -> list[str]:
        return self._pick_sentences(text, ["优先", "加分", "熟悉", "了解", "经验"], limit)

    def _pick_sentences(self, text: str, keywords: list[str], limit: int) -> list[str]:
        sentences = [item.strip() for item in re.split(r"[\n。；;]", text or "") if item.strip()]
        picked: list[str] = []
        for sentence in sentences:
            if any(keyword.lower() in sentence.lower() for keyword in keywords):
                picked.append(sentence[:160])
            if len(picked) >= limit:
                break
        return picked


text_analyzer = TextAnalyzer()
