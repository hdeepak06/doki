from typing import List
from fastapi import APIRouter
from app.config import (
    settings,
    get_active_model,
    get_active_provider,
    save_persisted_settings
)
from app.schemas.models import ModelListResponse
from pydantic import BaseModel

router = APIRouter(prefix="/api/models", tags=["Models"])

class ModelSelectRequest(BaseModel):
    model: str

@router.get("", response_model=ModelListResponse)
async def get_available_models(refresh: bool = False):
    gemini_models = [
        {
            "id": "gemini-2.5-flash",
            "name": "Google Gemini 2.5 Flash (Primary Brain)",
            "description": "Google's next-gen multimodal reasoning flash model with zero-cost free tier",
            "context_length": 1048576,
            "is_free": True
        },
        {
            "id": "gemini-2.0-flash",
            "name": "Google Gemini 2.0 Flash",
            "description": "High-speed real-time multimodal intelligence from Google DeepMind",
            "context_length": 1048576,
            "is_free": True
        },
        {
            "id": "gemini-1.5-flash",
            "name": "Google Gemini 1.5 Flash",
            "description": "Fast and versatile performance across text and code on Google AI Studio",
            "context_length": 1048576,
            "is_free": True
        },
        {
            "id": "openrouter/free",
            "name": "OpenRouter Free Dynamic Router (Fallback)",
            "description": "Automatic router across free open-source community models",
            "context_length": 8192,
            "is_free": True
        },
        {
            "id": "meta-llama/llama-3.2-3b-instruct:free",
            "name": "Meta Llama 3.2 3B Instruct (Free)",
            "description": "Fast lightweight open-weights LLM by Meta",
            "context_length": 8192,
            "is_free": True
        },
        {
            "id": "qwen/qwen-2.5-coder-32b-instruct:free",
            "name": "Qwen 2.5 Coder 32B (Free)",
            "description": "Specialized open-source coding intelligence",
            "context_length": 32768,
            "is_free": True
        },
        {
            "id": "deepseek/deepseek-r1:free",
            "name": "DeepSeek R1 Reasoning (Free)",
            "description": "Deep reasoning and chain-of-thought model",
            "context_length": 16384,
            "is_free": True
        }
    ]

    active = get_active_model()
    return {
        "active_model": active,
        "models": gemini_models,
        "total": len(gemini_models)
    }

@router.post("/select")
async def select_active_model(req: ModelSelectRequest):
    # Enforce free model constraint
    is_gemini_free = req.model.startswith("gemini-")
    is_openrouter_free = (req.model == "openrouter/free" or req.model.endswith(":free"))

    if not (is_gemini_free or is_openrouter_free):
        return {"status": "error", "message": "Only free models are permitted on DOKI."}

    provider = "gemini" if is_gemini_free else "openrouter"
    save_persisted_settings({
        "active_model": req.model,
        "ai_primary_provider": provider
    })
    return {"status": "success", "active_model": req.model, "provider": provider}
