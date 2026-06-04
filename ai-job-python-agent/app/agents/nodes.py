from typing import Any

from app.agents.text_analyzer import text_analyzer
from app.ai.deepseek_client import deepseek_client
from app.models.job import JobPosting
from app.models.resume import ResumeProfile


def resume_parser_agent(resume: ResumeProfile) -> dict[str, Any]:
    skills = text_analyzer.find_skills(resume.content)
    projects = text_analyzer.extract_project_signals(resume.content)
    strengths: list[str] = []
    if "Python" in skills and "FastAPI" in skills:
        strengths.append("具备 Python Web 后端开发基础")
    if "MySQL" in skills and "Redis" in skills:
        strengths.append("有数据库和缓存相关项目表达")
    if "AI Agent" in skills or "LangGraph" in skills:
        strengths.append("项目能体现大模型应用和 Agent 工作流能力")
    if not strengths:
        strengths.append("简历中已有可用于岗位匹配的项目和技能信息")

    summary = deepseek_client.complete(
        "resume-parser: summarize a resume for internship matching in Chinese.",
        resume.content,
    )
    return {"skills": skills, "projects": projects, "strengths": strengths, "ai_summary": summary}


def job_analyzer_agent(job: JobPosting) -> dict[str, Any]:
    required_skills = text_analyzer.find_skills(job.description)
    responsibilities = text_analyzer.extract_responsibilities(job.description)
    bonus_items = text_analyzer.extract_bonus_items(job.description)
    summary = deepseek_client.complete(
        "jd-analyzer: summarize a job description for internship matching in Chinese.",
        job.description,
    )
    return {
        "required_skills": required_skills,
        "responsibilities": responsibilities,
        "bonus_items": bonus_items,
        "ai_summary": summary,
    }


def match_scoring_agent(resume: dict[str, Any], job: dict[str, Any]) -> dict[str, Any]:
    resume_skills = list(dict.fromkeys(resume.get("skills", [])))
    required_skills = list(dict.fromkeys(job.get("required_skills", [])))
    matched = [skill for skill in required_skills if skill in resume_skills]
    missing = [skill for skill in required_skills if skill not in resume_skills]
    base_score = 65 if not required_skills else 35 + len(matched) * 55 // max(len(required_skills), 1)
    project_bonus = min(10, len(resume.get("projects", [])) * 2)
    score = max(0, min(100, base_score + project_bonus))

    risks = []
    if missing:
        risks.append("JD 中出现但简历未明显体现：" + "、".join(missing))
    if len(resume.get("projects", [])) < 2:
        risks.append("项目数量或项目细节偏少，建议补充更贴近目标岗位的项目表达")
    if "MySQL" not in resume_skills:
        risks.append("数据库能力是后端实习常见考点，建议体现表设计、索引或事务处理")
    if not risks:
        risks.append("主要风险较低，重点准备项目细节追问")

    return {
        "score": score,
        "matched_skills": matched,
        "missing_skills": missing,
        "risks": risks,
        "reason": f"匹配技能 {len(matched)}/{max(len(required_skills), 1)}，项目表达带来 {project_bonus} 分加成。",
    }


def suggestion_agent(resume: dict[str, Any], job: dict[str, Any], match: dict[str, Any]) -> dict[str, Any]:
    improvements = [
        "把项目描述改成：业务背景 -> 技术方案 -> 结果指标，避免只罗列技术栈。",
        "针对 JD 高频词，在技能和项目中补齐同义表达，例如接口开发、缓存、数据库优化。",
    ]
    if match.get("missing_skills"):
        improvements.append("缺口技能需要单独补一条学习或项目实践：" + "、".join(match["missing_skills"]))

    ai_advice = deepseek_client.complete(
        "suggestion: generate resume improvement advice for an internship candidate in Chinese.",
        f"resume={resume}; job={job}; match={match}",
    )
    return {
        "resume_improvements": improvements,
        "project_rewrite_tips": [
            "为核心项目准备一个能讲 3 分钟的技术难点，例如 JWT 鉴权、Redis 缓存或 Agent 状态流转。",
            "补充量化结果，例如接口耗时下降、缓存命中率、重复调用减少、大模型调用成本降低。",
        ],
        "learning_plan": [
            f"补强 {skill}：先理解核心概念，再写到项目的具体功能点中。"
            for skill in match.get("missing_skills", [])
        ]
        or ["继续打磨已有项目，重点准备数据库、缓存、异步任务和异常处理追问。"],
        "ai_advice": ai_advice,
    }


def interview_agent(resume: dict[str, Any], job: dict[str, Any], match: dict[str, Any]) -> dict[str, Any]:
    questions = [
        "请介绍一个你最熟悉的后端项目，你负责了哪些模块？",
        "FastAPI 接口从请求进来到返回响应，中间大概经历了哪些过程？",
        "JWT 登录态如何生成、校验和过期处理？",
        "MySQL 索引为什么能提升查询性能？什么情况下索引会失效？",
        "Redis 缓存和数据库数据不一致时，你会怎么处理？",
        "你的 Agent 工作流为什么要拆成多个节点？每个节点的输入输出是什么？",
    ]
    if "AI Agent" in job.get("required_skills", []):
        questions.append("大模型返回格式不稳定时，你如何做结构化解析和兜底？")

    ai_advice = deepseek_client.complete(
        "interview: generate interview preparation advice for an internship candidate in Chinese.",
        f"requiredSkills={job.get('required_skills', [])}; risks={match.get('risks', [])}",
    )
    return {
        "questions": questions,
        "talking_points": [
            "先讲业务问题，再讲技术方案，最后讲结果和反思。",
            "把匹配技能串到项目里讲，不要只停留在技能列表。",
        ],
        "ai_advice": ai_advice,
    }
