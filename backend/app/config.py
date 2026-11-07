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
    
    # AI Providers Configuration
    AI_PRIMARY_PROVIDER: str = os.environ.get("AI_PRIMARY_PROVIDER", "gemini")
    AI_FREE_ONLY: bool = True
    AUTOMATIC_PROVIDER_FALLBACK: bool = True

    # Google Gemini Config (PRIMARY BRAIN)
    GEMINI_API_KEY: Optional[str] = os.environ.get("GEMINI_API_KEY")
    GEMINI_MODEL: str = os.environ.get("GEMINI_MODEL", "gemini-2.5-flash")
    GEMINI_FALLBACK_MODELS: List[str] = [
        "gemini-2.5-flash",
        "gemini-2.0-flash",
        "gemini-1.5-flash",
        "gemini-1.5-flash-8b"
    ]
    
    # OpenRouter Config (SECONDARY FREE FALLBACK)
    OPENROUTER_BASE_URL: str = "https://openrouter.ai/api/v1"
    OPENROUTER_API_KEY: Optional[str] = os.environ.get("OPENROUTER_API_KEY")
    OPENROUTER_MODEL: str = os.environ.get("OPENROUTER_MODEL", "openrouter/free")
    OPENROUTER_FALLBACK_MODELS: List[str] = [
        "openrouter/free",
        "meta-llama/llama-3.2-3b-instruct:free",
        "google/gemini-2.0-flash-lite:free",
        "qwen/qwen-2.5-coder-32b-instruct:free",
        "deepseek/deepseek-r1:free",
        "mistralai/mistral-7b-instruct:free"
    ]
    
    # Backward compatibility
    DEFAULT_MODEL: str = "gemini-2.5-flash"
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
        print(f"Warning: Could not persist settings to file: {e}")

def get_effective_gemini_key() -> str:
    """Retrieves Google Gemini API key from environment or local persisted settings."""
    env_key = os.environ.get("GEMINI_API_KEY") or settings.GEMINI_API_KEY
    if env_key and len(env_key.strip()) > 5:
        return env_key.strip()
    persisted = get_persisted_settings()
    if persisted.get("gemini_api_key"):
        return persisted["gemini_api_key"].strip()
    return ""

def get_effective_openrouter_key() -> str:
    """Retrieves OpenRouter API key from environment or local persisted settings."""
    env_key = os.environ.get("OPENROUTER_API_KEY") or settings.OPENROUTER_API_KEY
    if env_key and len(env_key.strip()) > 5:
        return env_key.strip()
    persisted = get_persisted_settings()
    if persisted.get("openrouter_api_key"):
        return persisted["openrouter_api_key"].strip()
    return ""

# Alias for backward-compatibility
def get_effective_api_key() -> str:
    return get_effective_gemini_key() or get_effective_openrouter_key()

def get_active_provider() -> str:
    persisted = get_persisted_settings()
    return persisted.get("ai_primary_provider", settings.AI_PRIMARY_PROVIDER)

def get_active_model() -> str:
    persisted = get_persisted_settings()
    provider = get_active_provider()
    if provider == "gemini":
        return persisted.get("gemini_model", settings.GEMINI_MODEL)
    return persisted.get("active_model", settings.OPENROUTER_MODEL)

def is_free_only_mode() -> bool:
    persisted = get_persisted_settings()
    return persisted.get("free_only_mode", True)

def is_auto_fallback_enabled() -> bool:
    persisted = get_persisted_settings()
    return persisted.get("auto_fallback_enabled", True)
