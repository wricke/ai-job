from sqlalchemy import Column, DateTime, Integer, String, Text, UniqueConstraint

from app.db.session import Base


class JobPosting(Base):
    __tablename__ = "job_posting"
    __table_args__ = (UniqueConstraint("user_id", "title", "source", name="uk_job_user_title_source"),)

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, nullable=False, index=True)
    company = Column(String(120))
    title = Column(String(120), nullable=False)
    source = Column(String(120))
    description = Column(Text, nullable=False)
    created_at = Column(DateTime, nullable=False)
    updated_at = Column(DateTime, nullable=False)
