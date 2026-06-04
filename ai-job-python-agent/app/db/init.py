from app.db.session import Base, SessionLocal, engine
from app.models import analysis, job, resume, user  # noqa: F401
from app.services.sample_data_service import ensure_demo_account


def init_db() -> None:
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    try:
        ensure_demo_account(db)
    finally:
        db.close()
