from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from pydantic import BaseModel
from app.database import get_db
from app.models.memory import Memory
from app.services.memory_service import MemoryService

router = APIRouter(prefix="/api/memory", tags=["Memory"])

class MemoryCreateRequest(BaseModel):
    key: str
    value: str
    category: str = "general"

@router.get("")
def list_memories(category: Optional[str] = None, q: Optional[str] = None, db: Session = Depends(get_db)):
    query = db.query(Memory)
    if category and category != "all":
        query = query.filter(Memory.category == category)
    if q:
        query = query.filter((Memory.key.ilike(f"%{q}%")) | (Memory.value.ilike(f"%{q}%")))
    
    memories = query.order_by(Memory.updated_at.desc()).all()
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

@router.post("")
def create_or_update_memory(req: MemoryCreateRequest, db: Session = Depends(get_db)):
    return MemoryService.save_memory(
        session=db,
        key=req.key.strip(),
        value=req.value.strip(),
        category=req.category.strip()
    )

@router.delete("/{memory_id}")
def delete_memory(memory_id: str, db: Session = Depends(get_db)):
    mem = db.query(Memory).filter(Memory.id == memory_id).first()
    if not mem:
        raise HTTPException(status_code=404, detail="Memory not found")
    MemoryService.delete_memory(db, memory_id)
    return {"status": "success", "message": "Memory deleted"}

