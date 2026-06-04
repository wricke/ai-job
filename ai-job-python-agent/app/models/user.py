from sqlalchemy import Column, DateTime, Integer, String

from app.db.session import Base


class UserAccount(Base):
    __tablename__ = "user_account"

    id = Column(Integer, primary_key=True, autoincrement=True)
    username = Column(String(80), nullable=False, unique=True, index=True)
    password_hash = Column(String(255), nullable=False)
    display_name = Column(String(80))
    created_at = Column(DateTime, nullable=False)
    updated_at = Column(DateTime, nullable=False)
