from datetime import datetime
import uuid
from sqlalchemy import Column, String, DateTime
from app.database import Base

class Device(Base):
    __tablename__ = "devices"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    device_id = Column(String(100), unique=True, nullable=False)
    name = Column(String(100), nullable=False)
    device_type = Column(String(20), nullable=False)  # 'laptop' or 'android'
    ip_address = Column(String(45), nullable=True)
    status = Column(String(20), default="offline")  # 'online' or 'offline'
    battery_level = Column(String(10), nullable=True)
    last_seen = Column(DateTime, default=datetime.utcnow)
    paired_at = Column(DateTime, default=datetime.utcnow)

