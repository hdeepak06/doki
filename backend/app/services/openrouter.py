# Backward-compatibility bridge for ai_service
from app.services.ai_service import (
    AIService,
    ai_service,
    openrouter_service
)

__all__ = ["AIService", "ai_service", "openrouter_service"]
