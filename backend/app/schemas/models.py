from typing import List, Optional
from pydantic import BaseModel

class ModelInfo(BaseModel):
    id: str
    name: str
    description: Optional[str] = ""
    context_length: Optional[int] = 4096
    pricing: Optional[dict] = None
    is_free: bool = True

class ModelListResponse(BaseModel):
    active_model: str
    models: List[ModelInfo]
    total: int

