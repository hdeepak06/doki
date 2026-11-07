from typing import Optional
from pydantic import BaseModel

class ConfigUpdateRequest(BaseModel):
    gemini_api_key: Optional[str] = None
    openrouter_api_key: Optional[str] = None
    ai_primary_provider: Optional[str] = None
    active_model: Optional[str] = None
    gemini_model: Optional[str] = None
    assistant_name: Optional[str] = None
    voice_speed: Optional[float] = None
    voice_language: Optional[str] = None
    free_only_mode: Optional[bool] = None
    auto_fallback_enabled: Optional[bool] = None

class ConfigResponse(BaseModel):
    has_api_key: bool
    masked_api_key: str
    has_gemini_key: bool
    masked_gemini_key: str
    has_openrouter_key: bool
    masked_openrouter_key: str
    primary_provider: str = "gemini"
    active_provider: str = "gemini"
    primary_model: str = "gemini-2.5-flash"
    fallback_model: str = "openrouter/free"
    active_model: str
    assistant_name: str
    owner_name: str
    version: str
    ai_brain_status: str = "INITIALIZING"
    mode: str = "FREE"
    automatic_fallback: bool = True
    last_successful_model: str = "gemini-2.5-flash"
