import asyncio
import sys
from pathlib import Path

# Add backend to path
sys.path.insert(0, str(Path(__file__).parent))

from app.api.config import get_config
from app.api.devices import get_devices
from app.services.openrouter import openrouter_service

async def main():
    print("[1] Testing get_config...")
    cfg = await get_config()
    print("CONFIG:", cfg)

    print("\n[2] Testing get_devices...")
    dev = await get_devices()
    print("DEVICES:", dev)

    print("\n[3] Testing openrouter free models...")
    models = await openrouter_service.fetch_models()
    print(f"MODELS: {len(models)} free models available")
    for m in models[:3]:
        print(f" - {m['id']}: {m['name']}")

    print("\nALL STAGE 1 BACKEND TESTS PASSED!")

if __name__ == "__main__":
    asyncio.run(main())

