# Forward all services for direct imports
from app.services.openrouter import openrouter_service, OpenRouterService
from app.services.memory_service import MemoryService
from app.services.websocket_manager import ws_manager, WebSocketManager
from app.services.kannada_service import kannada_service, KannadaIntelligenceService

__all__ = [
    "openrouter_service",
    "OpenRouterService",
    "MemoryService",
    "ws_manager",
    "WebSocketManager",
    "kannada_service",
    "KannadaIntelligenceService"
]
