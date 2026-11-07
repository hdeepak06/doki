import uvicorn
from app.config import settings

if __name__ == "__main__":
    print(f"""
    ==================================================
           DOKI - PERSONAL JARVIS AI ASSISTANT        
           Owner: {settings.OWNER_NAME}               
           Host: http://localhost:{settings.PORT}     
           API Docs: http://localhost:{settings.PORT}/docs
    ==================================================
    """)
    uvicorn.run("app.main:app", host=settings.HOST, port=settings.PORT, reload=False)

