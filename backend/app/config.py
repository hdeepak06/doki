import os
import json
import tempfile
from pathlib import Path
from typing import List, Optional
from pydantic_settings import BaseSettings

BASE_DIR = Path(__file__).resolve().parent.parent

# In Vercel serverless / AWS Lambda, use system temp directory (read-only filesystem bypass)
if os.environ.get("VERCEL") or os.environ.get("AWS_LAMBDA_FUNCTION_NAME"):
    DATA_DIR = Path(tempfile.gettempdir()) / "doki_data"
else:
    DATA_DIR = BASE_DIR / "data"

DATA_DIR.mkdir(parents=True, exist_ok=True)
SETTINGS_FILE = DATA_DIR / "settings.json"

class Settings(BaseSettings):
    PROJECT_NAME: str = "DOKI"
    ASSISTANT_NAME: str = "DOKI"
    OWNER_NAME: str = "Deepak"
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    APP_ENV: str = os.environ.get("APP_ENV", "production")
    FRONTEND_ORIGIN: str = os.environ.get("FRONTEND_ORIGIN", "*")
    
    # OpenRouter Config
    OPENROUTER_BASE_URL: str = "https://openrouter.ai/api/v1"
    OPENROUTER_API_KEY: Optional[str] = os.environ.get("OPENROUTER_API_KEY")
    OPENROUTER_MODEL: Optional[str] = os.environ.get("OPENROUTER_MODEL")
    DEFAULT_MODEL: str = os.environ.get("OPENROUTER_MODEL", "openrouter/free")
    
    # Free model fallbacks (always ending in :free or openrouter/free)
    FALLBACK_MODELS: List[str] = [
        "openrouter/free",
        "meta-llama/llama-3.2-3b-instruct:free",
        "google/gemini-2.0-flash-lite:free",
        "qwen/qwen-2.5-coder-32b-instruct:free",
        "deepseek/deepseek-r1:free",
        "mistralai/mistral-7b-instruct:free"
    ]
    
    # Database URL
    DATABASE_URL: str = f"sqlite:///{(DATA_DIR / 'doki.db').as_posix()}"
    
    # CORS
    CORS_ORIGINS: List[str] = ["*"]

    class Config:
        env_file = str(BASE_DIR / ".env")
        env_file_encoding = "utf-8"
        extra = "allow"

settings = Settings()

# Support reading runtime overrides from data/settings.json
def get_persisted_settings() -> dict:
    if SETTINGS_FILE.exists():
        try:
            with open(SETTINGS_FILE, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            return {}
    return {}

def save_persisted_settings(data: dict) -> None:
    try:
        current = get_persisted_settings()
        current.update(data)
        with open(SETTINGS_FILE, "w", encoding="utf-8") as f:
            json.dump(current, f, indent=2)
    except Exception as e:
        # Graceful fallback if filesystem is read-only
        print(f"Warning: Could not persist settings to file: {e}")

def get_effective_api_key() -> str:
    # 1. Environment variable (Vercel Production / CI)
    env_key = os.environ.get("OPENROUTER_API_KEY") or settings.OPENROUTER_API_KEY
    if env_key and len(env_key.strip()) > 5:
        return env_key.strip()
    # 2. Local persisted settings file
    persisted = get_persisted_settings()
    if persisted.get("openrouter_api_key"):
        return persisted["openrouter_api_key"].strip()
    return ""

def get_active_model() -> str:
    # 1. Environment variable override
    env_model = os.environ.get("OPENROUTER_MODEL") or settings.OPENROUTER_MODEL
    if env_model:
        return env_model.strip()
    # 2. Local persisted settings override
    persisted = get_persisted_settings()
    return persisted.get("active_model", settings.DEFAULT_MODEL)
