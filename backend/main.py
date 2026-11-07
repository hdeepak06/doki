import sys
from pathlib import Path

# Ensure backend root is always on sys.path
backend_dir = Path(__file__).resolve().parent
if str(backend_dir) not in sys.path:
    sys.path.insert(0, str(backend_dir))

# Import FastAPI ASGI application
from app.main import app

# Vercel serverless handler alias
handler = app

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)

