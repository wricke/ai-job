from sqlalchemy import Column, DateTime, Integer, String, Text

from app.db.session import Base


class AnalysisTask(Base):
    __tablename__ = "analysis_task"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, nullable=False, index=True)
    resume_id = Column(Integer, nullable=False, index=True)
    job_id = Column(Integer, nullable=False, index=True)
    status = Column(String(20), nullable=False, index=True)
    match_score = Column(Integer)
    summary = Column(Text)
    resume_insight = Column(Text)
    job_insight = Column(Text)
    match_detail = Column(Text)
    suggestions = Column(Text)
    interview_questions = Column(Text)
    agent_trace = Column(Text)
    error_message = Column(Text)
    cache_key = Column(String(128), nullable=False, index=True)
    created_at = Column(DateTime, nullable=False)
    updated_at = Column(DateTime, nullable=False)
    completed_at = Column(DateTime)


class AnalysisCache(Base):
    __tablename__ = "analysis_cache"

    cache_key = Column(String(128), primary_key=True)
    match_score = Column(Integer, nullable=False)
    summary = Column(Text, nullable=False)
    resume_insight = Column(Text, nullable=False)
    job_insight = Column(Text, nullable=False)
    match_detail = Column(Text, nullable=False)
    suggestions = Column(Text, nullable=False)
    interview_questions = Column(Text, nullable=False)
    agent_trace = Column(Text, nullable=False)
    created_at = Column(DateTime, nullable=False)
    updated_at = Column(DateTime, nullable=False)
