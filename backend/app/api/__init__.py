from app.api.chat import router as chat_router
from app.api.models import router as models_router
from app.api.config import router as config_router
from app.api.devices import router as devices_router
from app.api.ws import router as ws_router
from app.api.research import router as research_router
from app.api.memory import router as memory_router

__all__ = [
    "chat_router",
    "models_router",
    "config_router",
    "devices_router",
    "ws_router",
    "research_router",
    "memory_router"
]
