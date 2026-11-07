from fastapi import APIRouter
from app.config import (
    settings,
    get_effective_api_key,
    get_active_model,
    save_persisted_settings,
    get_persisted_settings
)
from app.schemas.config import ConfigUpdateRequest, ConfigResponse
from app import __version__

router = APIRouter(prefix="/api/config", tags=["Configuration"])

@router.get("", response_model=ConfigResponse)
async def get_config():
    key = get_effective_api_key()
    has_key = bool(key and len(key.strip()) > 5)
    masked = f"{key[:4]}...{key[-4:]}" if has_key and len(key) > 8 else ("Configured" if has_key else "Not Set")
    
    return {
        "has_api_key": has_key,
        "masked_api_key": masked,
        "active_model": get_active_model(),
        "assistant_name": settings.ASSISTANT_NAME,
        "owner_name": settings.OWNER_NAME,
        "version": __version__
    }

@router.post("")
async def update_config(req: ConfigUpdateRequest):
    updates = {}
    if req.openrouter_api_key is not None:
        key_cleaned = req.openrouter_api_key.strip()
        updates["openrouter_api_key"] = key_cleaned
    if req.active_model:
        updates["active_model"] = req.active_model
    if req.assistant_name:
        updates["assistant_name"] = req.assistant_name
    if req.voice_speed is not None:
        updates["voice_speed"] = req.voice_speed
    if req.voice_language:
        updates["voice_language"] = req.voice_language

    save_persisted_settings(updates)
    return {"status": "success", "message": "Settings saved securely."}

