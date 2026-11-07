from typing import Optional
from fastapi import APIRouter, Query
from app.config import (
    settings,
    get_effective_gemini_key,
    get_effective_openrouter_key,
    get_effective_api_key,
    get_active_provider,
    get_active_model,
    is_free_only_mode,
    is_auto_fallback_enabled,
    save_persisted_settings,
    get_persisted_settings
)
from app.schemas.config import ConfigUpdateRequest, ConfigResponse
from app.services.ai_service import ai_service
from app import __version__

router = APIRouter(prefix="/api/config", tags=["Configuration"])

@router.get("", response_model=ConfigResponse)
async def get_config():
    g_key = get_effective_gemini_key()
    o_key = get_effective_openrouter_key()
    has_g = bool(g_key and len(g_key.strip()) > 5)
    has_o = bool(o_key and len(o_key.strip()) > 5)
    
    masked_g = f"{g_key[:4]}...{g_key[-4:]}" if has_g and len(g_key) > 8 else ("Configured" if has_g else "Not Set")
    masked_o = f"{o_key[:4]}...{o_key[-4:]}" if has_o and len(o_key) > 8 else ("Configured" if has_o else "Not Set")

    primary_provider = get_active_provider()
    active_prov = "gemini" if has_g else ("openrouter" if has_o else "gemini")
    
    status = "CONNECTED" if (has_g or has_o) else "MISSING_KEY"

    return {
        "has_api_key": has_g or has_o,
        "masked_api_key": masked_g if has_g else masked_o,
        "has_gemini_key": has_g,
        "masked_gemini_key": masked_g,
        "has_openrouter_key": has_o,
        "masked_openrouter_key": masked_o,
        "primary_provider": primary_provider,
        "active_provider": active_prov,
        "primary_model": settings.GEMINI_MODEL,
        "fallback_model": settings.OPENROUTER_MODEL,
        "active_model": settings.GEMINI_MODEL if active_prov == "gemini" else settings.OPENROUTER_MODEL,
        "assistant_name": settings.ASSISTANT_NAME,
        "owner_name": settings.OWNER_NAME,
        "version": __version__,
        "ai_brain_status": status,
        "mode": "FREE",
        "automatic_fallback": is_auto_fallback_enabled(),
        "last_successful_model": ai_service.last_model_used
    }

@router.get("/check")
async def check_ai_connection(
    gemini_key: Optional[str] = Query(None),
    openrouter_key: Optional[str] = Query(None)
):
    """Live connectivity verification for Google Gemini (Primary) and OpenRouter (Fallback)."""
    return await ai_service.check_connectivity(gemini_key=gemini_key, openrouter_key=openrouter_key)

@router.post("")
async def update_config(req: ConfigUpdateRequest):
    updates = {}
    if req.gemini_api_key is not None:
        updates["gemini_api_key"] = req.gemini_api_key.strip()
    if req.openrouter_api_key is not None:
        updates["openrouter_api_key"] = req.openrouter_api_key.strip()
    if req.ai_primary_provider:
        updates["ai_primary_provider"] = req.ai_primary_provider
    if req.gemini_model:
        updates["gemini_model"] = req.gemini_model
    if req.active_model:
        updates["active_model"] = req.active_model
    if req.assistant_name:
        updates["assistant_name"] = req.assistant_name
    if req.voice_speed is not None:
        updates["voice_speed"] = req.voice_speed
    if req.voice_language:
        updates["voice_language"] = req.voice_language
    if req.free_only_mode is not None:
        updates["free_only_mode"] = req.free_only_mode
    if req.auto_fallback_enabled is not None:
        updates["auto_fallback_enabled"] = req.auto_fallback_enabled

    save_persisted_settings(updates)
    return {"status": "success", "message": "Settings saved securely."}
