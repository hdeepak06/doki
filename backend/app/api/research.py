from typing import Optional
from fastapi import APIRouter, Query, Header
from pydantic import BaseModel
from app.services.research_service import research_service

router = APIRouter(prefix="/api/research", tags=["Internet Research"])

class ResearchRequest(BaseModel):
    query: str
    api_key: Optional[str] = None

@router.post("")
async def run_research(req: ResearchRequest, x_openrouter_key: Optional[str] = Header(None)):
    key = req.api_key or x_openrouter_key
    return await research_service.conduct_research(query=req.query, api_key=key)

@router.get("/quick")
async def quick_search(q: str = Query(..., min_length=2)):
    sources = await research_service.search_web(q, max_results=5)
    return {"query": q, "results": sources}

