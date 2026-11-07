from typing import Optional
from pydantic import BaseModel

class ConfigUpdateRequest(BaseModel):
    openrouter_api_key: Optional[str] = None
    active_model: Optional[str] = None
    assistant_name: Optional[str] = None
    voice_speed: Optional[float] = None
    voice_language: Optional[str] = None

class ConfigResponse(BaseModel):
    has_api_key: bool
    masked_api_key: str
    active_model: str
    assistant_name: str
    owner_name: str
    version: str

