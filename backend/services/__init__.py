# Forward all services for direct imports
from app.services.ai_service import (
    AIService,
    ai_service,
    openrouter_service
)
from app.services.memory_service import MemoryService
from app.services.websocket_manager import ws_manager, ConnectionManager
from app.services.kannada_service import (
    kannada_service,
    KannadaIntelligenceService
)

__all__ = [
    "AIService",
    "ai_service",
    "openrouter_service",
    "MemoryService",
    "ws_manager",
    "ConnectionManager",
    "kannada_service",
    "KannadaIntelligenceService"
]
