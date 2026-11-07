import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import settings
from app.database import init_db
from app.api import (
    chat_router,
    models_router,
    config_router,
    devices_router,
    ws_router
)

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("doki.main")

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Initializing DOKI AI Assistant backend...")
    init_db()
    logger.info("DOKI Database tables initialized.")
    yield
    logger.info("Shutting down DOKI backend.")

app = FastAPI(
    title=settings.PROJECT_NAME,
    description="DOKI - Personal JARVIS-Style AI Assistant Backend for Deepak",
    version="1.0.0",
    lifespan=lifespan
)

# Enable CORS for local Vite frontend and mobile network access
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount API Routers
app.include_router(chat_router)
app.include_router(models_router)
app.include_router(config_router)
app.include_router(devices_router)
app.include_router(ws_router)

@app.get("/api/health")
async def health():
    return {
        "status": "online",
        "assistant": settings.ASSISTANT_NAME,
        "owner": settings.OWNER_NAME,
        "version": "1.0.0"
    }

@app.get("/")
async def root():
    return {
        "message": f"Welcome to {settings.ASSISTANT_NAME} AI Assistant.",
        "docs": "/docs",
        "health": "/api/health"
    }
