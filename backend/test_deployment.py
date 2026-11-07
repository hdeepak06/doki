import os
import sys
from pathlib import Path

# Simulate Vercel environment
os.environ["VERCEL"] = "1"
os.environ["APP_ENV"] = "production"

backend_path = Path(__file__).resolve().parent
sys.path.insert(0, str(backend_path))

import main
from app.config import DATA_DIR, get_effective_api_key, get_active_model

def run_tests():
    print("========================================================")
    print("     VERCEL MULTI-SERVICE BACKEND DEPLOYMENT TESTS      ")
    print("========================================================")

    # 1. Test entrypoint and handler
    print("\n[TEST 1] Testing backend/main.py entrypoint and handler...")
    assert hasattr(main, "app"), "main.py must expose 'app'"
    assert hasattr(main, "handler"), "main.py must expose 'handler'"
    print("✓ main:app and handler exposed successfully for Vercel")

    # 2. Check OpenAPI paths for /api and /api/health
    print("\n[TEST 2] Verifying FastAPI route registration under /api...")
    paths = list(main.app.openapi()["paths"].keys())
    print(f"Registered API paths ({len(paths)}): {paths}")
    assert "/api/health" in paths, "Must register /api/health"
    assert "/api" in paths, "Must register /api"
    assert "/api/chat/stream" in paths, "Must register /api/chat/stream"
    assert "/api/models" in paths, "Must register /api/models"
    assert "/api/config" in paths, "Must register /api/config"
    print("✓ All required API routes registered under /api prefix")
    print("✓ All required API routes registered under /api prefix")

    # 3. Test missing environment variables
    print("\n[TEST 3] Testing missing environment variables handling...")
    key = get_effective_api_key()
    model = get_active_model()
    print(f"API Key present: {bool(key)}, Active model: {model}")
    assert model == "openrouter/free"
    print("✓ Gracefully defaulted to free tier without crashing")

    # 4. Test Vercel temp directory database isolation
    print("\n[TEST 4] Testing Vercel temporary directory database isolation...")
    print("Active DATA_DIR under Vercel:", DATA_DIR)
    assert "temp" in str(DATA_DIR).lower() or "tmp" in str(DATA_DIR).lower()
    print("✓ SQLite isolated to temp directory for serverless read-only filesystem compatibility")

    print("\n========================================================")
    print("    ALL VERCEL BACKEND DEPLOYMENT TESTS PASSED!         ")
    print("========================================================")

if __name__ == "__main__":
    run_tests()
