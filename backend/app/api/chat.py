import json
from typing import List
from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session
from app.database import get_db, SessionLocal
from app.models.conversation import Conversation, Message
from app.schemas.chat import ChatRequest, ConversationResponse
from app.services.openrouter import openrouter_service
from app.services.memory_service import MemoryService

router = APIRouter(prefix="/api/chat", tags=["Chat"])

@router.post("/stream")
def stream_chat(req: ChatRequest, db: Session = Depends(get_db)):
    # 1. Get or create conversation
    conv = None
    if req.conversation_id:
        conv = db.query(Conversation).filter(Conversation.id == req.conversation_id).first()

    if not conv:
        snippet = req.message[:30] + ("..." if len(req.message) > 30 else "")
        conv = Conversation(title=snippet)
        db.add(conv)
        db.commit()
        db.refresh(conv)

    # 2. Persist user message
    user_msg = Message(
        conversation_id=conv.id,
        role="user",
        content=req.message
    )
    db.add(user_msg)
    db.commit()

    # 3. Fetch past messages for context
    history_records = db.query(Message).filter(Message.conversation_id == conv.id).order_by(Message.created_at).all()
    history = [{"role": m.role, "content": m.content} for m in history_records]

    # 4. Fetch memories
    memories = MemoryService.get_all_memories(db)
    conv_id = conv.id

    # 5. Generator that yields SSE and stores assistant response upon completion
    async def event_generator():
        accumulated_text = []
        used_model = req.model or "openrouter/free"

        async for chunk_json in openrouter_service.stream_chat(
            messages=history,
            model=req.model,
            memories=memories
        ):
            try:
                event = json.loads(chunk_json.strip())
                if event.get("type") == "start":
                    used_model = event.get("model", used_model)
                elif event.get("type") == "chunk":
                    accumulated_text.append(event.get("content", ""))
                elif event.get("type") == "end":
                    full_content = "".join(accumulated_text)
                    if full_content:
                        # Save assistant message using a fresh session
                        with SessionLocal() as save_db:
                            asst_msg = Message(
                                conversation_id=conv_id,
                                role="assistant",
                                content=full_content,
                                model=used_model
                            )
                            save_db.add(asst_msg)
                            save_db.commit()
            except Exception:
                pass

            yield f"data: {chunk_json}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Conversation-Id": conv_id
        }
    )

@router.get("/conversations", response_model=List[ConversationResponse])
def list_conversations(db: Session = Depends(get_db)):
    conversations = db.query(Conversation).order_by(Conversation.updated_at.desc()).all()
    return conversations

@router.get("/conversations/{conv_id}", response_model=ConversationResponse)
def get_conversation(conv_id: str, db: Session = Depends(get_db)):
    conv = db.query(Conversation).filter(Conversation.id == conv_id).first()
    if not conv:
        raise HTTPException(status_code=404, detail="Conversation not found")
    return conv

@router.delete("/conversations/{conv_id}")
def delete_conversation(conv_id: str, db: Session = Depends(get_db)):
    db.query(Message).filter(Message.conversation_id == conv_id).delete()
    db.query(Conversation).filter(Conversation.id == conv_id).delete()
    db.commit()
    return {"status": "success", "deleted_id": conv_id}

@router.delete("/conversations")
def clear_all_conversations(db: Session = Depends(get_db)):
    db.query(Message).delete()
    db.query(Conversation).delete()
    db.commit()
    return {"status": "success", "message": "All conversations cleared"}
