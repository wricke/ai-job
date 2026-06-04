from sqlalchemy.orm import Session

from app.core.security import hash_password
from app.models.analysis import AnalysisTask
from app.models.job import JobPosting
from app.models.resume import ResumeProfile
from app.models.user import UserAccount
from app.utils.json import dump_json
from app.utils.time import now


SAMPLE_RESUME_CONTENT = """教育背景：计算机相关专业，本科在读。
技能：Python、FastAPI、SQLAlchemy、MySQL、Redis、JWT、Docker、Git，了解 LangGraph、DeepSeek API 和大模型应用开发。
项目经历：职途雷达 AI 岗位匹配助手，负责用户登录鉴权、简历解析、岗位推荐、异步分析任务和多阶段 Agent 工作流。
项目基于 FastAPI + SQLAlchemy + MySQL + Redis + LangGraph + DeepSeek API 构建，支持 PDF/DOCX/TXT 简历解析、岗位匹配评分、优化建议和面试准备生成。
工程能力：熟悉 RESTful API 设计、Pydantic 参数校验、数据库表设计、缓存命中策略、异常兜底和接口联调。"""


SAMPLE_JOBS = [
    {
        "title": "Python 后端开发实习生",
        "score": 91,
        "level": "高",
        "matched": ["Python", "FastAPI", "SQLAlchemy", "MySQL", "JWT", "RESTful API"],
        "missing": ["接口压测", "线上排障", "事务一致性"],
        "keywords": ["Python 后端 实习", "FastAPI 实习", "后端开发 实习"],
        "tips": ["准备接口鉴权、数据库索引、异常处理和项目分层设计追问"],
    },
    {
        "title": "AI 应用开发实习生",
        "score": 84,
        "level": "中高",
        "matched": ["AI Agent", "LangGraph", "DeepSeek", "Prompt Engineering"],
        "missing": ["RAG", "向量数据库", "模型评测"],
        "keywords": ["AI 应用开发 实习", "大模型应用 实习", "Agent 开发"],
        "tips": ["说明 Agent 节点拆分、输入输出、失败兜底和结果持久化"],
    },
    {
        "title": "平台开发实习生",
        "score": 78,
        "level": "中",
        "matched": ["MySQL", "Redis", "Docker", "Git"],
        "missing": ["任务调度", "性能优化", "服务监控"],
        "keywords": ["平台开发 实习", "Python 开发 实习", "研发实习"],
        "tips": ["补充缓存命中、异步任务和数据表设计细节"],
    },
]


def ensure_demo_account(db: Session) -> None:
    user = db.query(UserAccount).filter(UserAccount.username == "demo").first()
    if user is None:
        timestamp = now()
        user = UserAccount(
            username="demo",
            password_hash=hash_password("demo123"),
            display_name="体验用户",
            created_at=timestamp,
            updated_at=timestamp,
        )
        db.add(user)
        db.commit()
        db.refresh(user)
    ensure_user_samples(db, user)


def ensure_user_samples(db: Session, user: UserAccount) -> None:
    resume = db.query(ResumeProfile).filter(ResumeProfile.user_id == user.id).order_by(ResumeProfile.id.asc()).first()
    if resume is None:
        timestamp = now()
        resume = ResumeProfile(
            user_id=user.id,
            title="Python 后端 / AI 应用开发实习简历样例",
            owner_name="候选人",
            target_role="Python 后端开发实习生",
            content=SAMPLE_RESUME_CONTENT,
            created_at=timestamp,
            updated_at=timestamp,
        )
        db.add(resume)
        db.commit()
        db.refresh(resume)

    sample_jobs = _ensure_sample_jobs(db, user.id, resume)
    _ensure_sample_analysis(db, user.id, resume, sample_jobs[0])


def _ensure_sample_jobs(db: Session, user_id: int, resume: ResumeProfile) -> list[JobPosting]:
    jobs: list[JobPosting] = []
    timestamp = now()
    for item in SAMPLE_JOBS:
        source = f"AI 推荐 / 简历 {resume.id}"
        job = (
            db.query(JobPosting)
            .filter(JobPosting.user_id == user_id, JobPosting.title == item["title"], JobPosting.source == source)
            .first()
        )
        if job is None:
            job = JobPosting(user_id=user_id, created_at=timestamp)
            db.add(job)
        job.company = "系统样例"
        job.title = item["title"]
        job.source = source
        job.description = _job_description(item)
        job.updated_at = timestamp
        jobs.append(job)
    db.commit()
    for job in jobs:
        db.refresh(job)
    return jobs


def _ensure_sample_analysis(db: Session, user_id: int, resume: ResumeProfile, job: JobPosting) -> None:
    existing = (
        db.query(AnalysisTask)
        .filter(AnalysisTask.user_id == user_id, AnalysisTask.resume_id == resume.id, AnalysisTask.job_id == job.id)
        .first()
    )
    if existing is not None:
        return

    timestamp = now()
    match_detail = {
        "score": 91,
        "matchedSkills": ["Python", "FastAPI", "SQLAlchemy", "MySQL", "JWT", "RESTful API"],
        "missingSkills": ["接口压测", "线上排障", "事务一致性"],
        "risks": ["建议补充接口耗时、缓存命中率和数据库索引优化等量化指标"],
        "reason": "默认样例：简历技能与 Python 后端开发岗位高度匹配。",
    }
    suggestions = {
        "resumeImprovements": [
            "把项目描述整理成：业务背景 -> 技术方案 -> 结果指标。",
            "补充 FastAPI 接口鉴权、数据库表设计和 Redis 缓存策略的实现细节。",
        ],
        "projectRewriteTips": [
            "为 Agent 工作流准备 1 个能讲 3 分钟的技术难点。",
            "补充大模型调用失败后的兜底策略和结果缓存逻辑。",
        ],
        "learningPlan": ["补强接口压测、线上排障和事务一致性场景。"],
        "aiAdvice": "默认样例建议：重点突出 Python 后端工程能力和 AI 应用落地能力。",
    }
    interview = {
        "questions": [
            "FastAPI 接口从请求进来到响应返回，中间经历了哪些过程？",
            "JWT 是如何生成、校验和过期处理的？",
            "为什么要把求职分析拆成多个 Agent 节点？",
            "Redis 缓存和 MySQL 数据不一致时如何处理？",
        ],
        "talkingPoints": ["先讲业务问题，再讲技术方案，最后讲结果和反思。"],
        "aiAdvice": "默认样例建议：准备接口鉴权、数据库设计、缓存、Agent 工作流和大模型异常兜底。",
    }
    trace = [
        {"agentName": "ResumeParserAgent", "status": "SUCCESS", "durationMs": 180, "message": "step completed"},
        {"agentName": "JobAnalyzerAgent", "status": "SUCCESS", "durationMs": 160, "message": "step completed"},
        {"agentName": "MatchScoringAgent", "status": "SUCCESS", "durationMs": 12, "message": "step completed"},
        {"agentName": "SuggestionAgent", "status": "SUCCESS", "durationMs": 220, "message": "step completed"},
        {"agentName": "InterviewAgent", "status": "SUCCESS", "durationMs": 210, "message": "step completed"},
    ]

    task = AnalysisTask(
        user_id=user_id,
        resume_id=resume.id,
        job_id=job.id,
        status="COMPLETED",
        match_score=91,
        summary="默认样例：当前简历与 Python 后端开发实习生岗位匹配度 91 分，优势集中在 FastAPI、SQLAlchemy、MySQL、JWT 和 RESTful API。",
        resume_insight=dump_json(
            {
                "skills": ["Python", "FastAPI", "SQLAlchemy", "MySQL", "Redis", "JWT", "AI Agent"],
                "projects": ["职途雷达 AI 岗位匹配助手"],
                "strengths": ["具备 Python Web 后端开发基础", "项目能体现大模型应用和 Agent 工作流能力"],
                "aiSummary": "默认样例：候选人具备后端接口、数据持久化和 AI 应用项目经验。",
            }
        ),
        job_insight=dump_json(
            {
                "requiredSkills": ["Python", "FastAPI", "MySQL", "JWT", "RESTful API"],
                "responsibilities": ["负责后端接口开发、数据库设计和服务联调"],
                "bonusItems": ["了解 Redis、Docker 和大模型应用开发"],
                "aiSummary": "默认样例：岗位关注 Python 后端工程能力和项目落地能力。",
            }
        ),
        match_detail=dump_json(match_detail),
        suggestions=dump_json(suggestions),
        interview_questions=dump_json(interview),
        agent_trace=dump_json(trace),
        cache_key=f"sample-{user_id}-{resume.id}-{job.id}",
        created_at=timestamp,
        updated_at=timestamp,
        completed_at=timestamp,
    )
    db.add(task)
    db.commit()


def _job_description(item: dict) -> str:
    return "\n".join(
        [
            f"Role: {item['title']}",
            f"Fit score: {item['score']} ({item['level']})",
            "Matched skills:",
            *[f"- {value}" for value in item["matched"]],
            "Missing skills:",
            *[f"- {value}" for value in item["missing"]],
            "Search keywords:",
            *[f"- {value}" for value in item["keywords"]],
            "Preparation tips:",
            *[f"- {value}" for value in item["tips"]],
        ]
    )
