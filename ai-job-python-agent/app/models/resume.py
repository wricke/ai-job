from sqlalchemy import Column, DateTime, Integer, String, Text

from app.db.session import Base


class ResumeProfile(Base):
    __tablename__ = "resume_profile"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, nullable=False, index=True)
    title = Column(String(120), nullable=False)
    owner_name = Column(String(80))
    target_role = Column(String(120))
    content = Column(Text, nullable=False)
    created_at = Column(DateTime, nullable=False)
    updated_at = Column(DateTime, nullable=False)
