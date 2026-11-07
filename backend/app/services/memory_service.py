from typing import List
from sqlalchemy.orm import Session
from app.models.memory import Memory

class MemoryService:
    @staticmethod
    def get_all_memories(session: Session) -> List[dict]:
        memories = session.query(Memory).order_by(Memory.updated_at.desc()).all()
        return [
            {
                "id": m.id,
                "key": m.key,
                "value": m.value,
                "category": m.category,
                "created_at": m.created_at.isoformat() if m.created_at else "",
                "updated_at": m.updated_at.isoformat() if m.updated_at else ""
            }
            for m in memories
        ]

    @staticmethod
    def save_memory(session: Session, key: str, value: str, category: str = "general") -> dict:
        target = session.query(Memory).filter(Memory.key == key).first()
        if target:
            target.value = value
            target.category = category
        else:
            target = Memory(key=key, value=value, category=category)
            session.add(target)
        
        session.commit()
        session.refresh(target)
        return {
            "id": target.id,
            "key": target.key,
            "value": target.value,
            "category": target.category
        }

    @staticmethod
    def delete_memory(session: Session, memory_id: str) -> bool:
        session.query(Memory).filter(Memory.id == memory_id).delete()
        session.commit()
        return True
