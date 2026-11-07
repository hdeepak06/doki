from datetime import datetime
import uuid
from sqlalchemy import Column, String, Text, DateTime
from app.database import Base

class Memory(Base):
    __tablename__ = "memories"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    key = Column(String(100), nullable=False, index=True)
    value = Column(Text, nullable=False)
    category = Column(String(50), default="general")  # preferences, projects, facts, instructions
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

