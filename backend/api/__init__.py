# Forward all router exports for direct imports
from app.api import (
    chat_router,
    models_router,
    config_router,
    devices_router,
    ws_router
)

__all__ = [
    "chat_router",
    "models_router",
    "config_router",
    "devices_router",
    "ws_router"
]
