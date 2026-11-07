import json
import os
import subprocess
import psutil
from typing import List
from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session
from app.database import get_db, SessionLocal
from app.models.conversation import Conversation, Message
from app.schemas.chat import ChatRequest, ConversationResponse
from app.services.ai_service import ai_service
from app.services.memory_service import MemoryService
from app.services.kannada_service import kannada_service
from app.services.websocket_manager import ws_manager

router = APIRouter(prefix="/api/chat", tags=["Chat"])

def execute_direct_intent(parsed_cmd: dict, is_native_kannada: bool) -> str:
    action = parsed_cmd["action"]
    target = parsed_cmd.get("target_device", "laptop")
    params = parsed_cmd.get("params", {})

    if action == "get_system_info" or action == "check_ram":
        metric = params.get("metric", "ram")
        if metric == "battery":
            battery = psutil.sensors_battery()
            pct = f"{battery.percent:.0f}%" if battery else "100%"
            status = "ಪ್ಲಗ್ ಇನ್ ಆಗಿದೆ" if (battery and battery.power_plugged) else "ಬ್ಯಾಟರಿ ಮೇಲೆ ಕಾರ್ಯನಿರ್ವಹಿಸುತ್ತಿದೆ"
            if is_native_kannada:
                return f"ದೀಪಕ್ ಸರ್, ನಿಮ್ಮ ಬ್ಯಾಟರಿ ಚಾರ್ಜ್ {pct} ಇದೆ. ಸದ್ಯಕ್ಕೆ {status}."
            else:
                return f"Deepak sir, nimma battery {pct} ide. Charging status normal ide."
        else:
            mem = psutil.virtual_memory()
            used_gb = f"{mem.used / (1024**3):.1f} GB"
            total_gb = f"{mem.total / (1024**3):.1f} GB"
            pct = f"{mem.percent}%"
            if is_native_kannada:
                return f"ದೀಪಕ್ ಸರ್, ನಿಮ್ಮ ಲ್ಯಾಪ್ಟಾಪ್‌ನಲ್ಲಿ ಸದ್ಯಕ್ಕೆ {used_gb} RAM ಬಳಕೆಯಾಗಿದೆ (ಒಟ್ಟು {total_gb}, {pct})."
            else:
                return f"Deepak sir, nimma laptop nalli {used_gb} RAM use aagide ({pct})."

    elif action == "open_app":
        app_name = params.get("app_name", "").strip()
        if target == "android":
            ws_manager.send_to_device("android", {"action": "open_app", "params": {"package_or_name": app_name}})
            if is_native_kannada:
                return f"ದೀಪಕ್ ಸರ್, ನಿಮ್ಮ OPPO ಫೋನ್‌ನಲ್ಲಿ {app_name} ಅಪ್ಲಿಕೇಶನ್ ತೆರೆಯುವ ಆದೇಶ ಕಳುಹಿಸಲಾಗಿದೆ."
            else:
                return f"Deepak sir, nimma phone nalli {app_name} open maadalu request kaluhisiddene."
        else:
            # Safe Windows application launching
            safe_map = {
                "chrome": "start chrome",
                "google chrome": "start chrome",
                "vs code": "code",
                "vscode": "code",
                "calculator": "calc",
                "calc": "calc",
                "notepad": "notepad",
                "explorer": "explorer",
                "file explorer": "explorer",
                "settings": "start ms-settings:"
            }
            clean_key = app_name.lower().strip()
            cmd = safe_map.get(clean_key, f"start {clean_key}")
            try:
                subprocess.Popen(cmd, shell=True)
                if is_native_kannada:
                    return f"ಖಂಡಿತ ದೀಪಕ್ ಸರ್, ಲ್ಯಾಪ್ಟಾಪ್‌ನಲ್ಲಿ {app_name} ಅನ್ನು ತೆರೆಯುತ್ತಿದ್ದೇನೆ."
                else:
                    return f"Sure Deepak sir, laptop nalli {app_name} open maaduttiddene."
            except Exception as e:
                return f"ಅಪ್ಲಿಕೇಶನ್ ತೆರೆಯಲು ಸಾಧ್ಯವಾಗಲಿಲ್ಲ: {str(e)}"

    elif action == "lock_screen":
        try:
            subprocess.run("rundll32.exe user32.dll,LockWorkStation", shell=True)
            if is_native_kannada:
                return "ದೀಪಕ್ ಸರ್, ನಿಮ್ಮ ಲ್ಯಾಪ್ಟಾಪ್ ಸ್ಕ್ರೀನ್ ಲಾಕ್ ಮಾಡಲಾಗಿದೆ."
            else:
                return "Deepak sir, laptop screen lock maadalaagide."
        except Exception:
            return "Screen lock execution failed."

    return ""

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
        # Check direct Kannada / English hardware device command
        parsed_cmd = kannada_service.parse_kannada_command(req.message)
        if parsed_cmd:
            lang, _ = kannada_service.detect_language(req.message)
            is_native = (lang == "kn")
            direct_reply = execute_direct_intent(parsed_cmd, is_native)
            if direct_reply:
                yield f"data: {json.dumps({'type': 'start', 'model': 'doki-kannada-core'})}\n\n"
                yield f"data: {json.dumps({'type': 'chunk', 'content': direct_reply})}\n\n"
                yield f"data: {json.dumps({'type': 'end', 'model': 'doki-kannada-core'})}\n\n"
                with SessionLocal() as save_db:
                    asst_msg = Message(
                        conversation_id=conv_id,
                        role="assistant",
                        content=direct_reply,
                        model="doki-kannada-core"
                    )
                    save_db.add(asst_msg)
                    save_db.commit()
                return

        accumulated_text = []
        used_model = req.model or "gemini-2.5-flash"

        # Resolve explicit request key or environment key
        client_key = req.api_key.strip() if (req.api_key and len(req.api_key.strip()) > 8) else None

        async for chunk_json in ai_service.stream_chat(
            messages=history,
            model=req.model,
            memories=memories,
            api_key=client_key
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
