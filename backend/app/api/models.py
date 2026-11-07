from fastapi import APIRouter
from app.services.openrouter import openrouter_service
from app.config import get_active_model, save_persisted_settings
from app.schemas.models import ModelListResponse
from pydantic import BaseModel

router = APIRouter(prefix="/api/models", tags=["Models"])

class ModelSelectRequest(BaseModel):
    model: str

@router.get("", response_model=ModelListResponse)
async def get_available_models(refresh: bool = False):
    models = await openrouter_service.fetch_models(force_refresh=refresh)
    active = get_active_model()
    return {
        "active_model": active,
        "models": models,
        "total": len(models)
    }

@router.post("/select")
async def select_active_model(req: ModelSelectRequest):
    # Enforce free model constraint
    if not (req.model == "openrouter/free" or req.model.endswith(":free")):
        return {"status": "error", "message": "Only free models are permitted on DOKI."}

    save_persisted_settings({"active_model": req.model})
    return {"status": "success", "active_model": req.model}

