import re
import urllib.parse
from html.parser import HTMLParser
from typing import List, Dict, Optional
import httpx
from app.services.ai_service import ai_service
import logging

logger = logging.getLogger("doki.research")

class DuckDuckGoHTMLParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.results = []
        self._current_result = None
        self._in_a = False
        self._in_snippet = False
        self._class_stack = []

    def handle_starttag(self, tag, attrs):
        attrs_dict = dict(attrs)
        classes = attrs_dict.get("class", "").split()
        
        if "result" in classes:
            if self._current_result and self._current_result.get("title"):
                self.results.append(self._current_result)
            self._current_result = {"title": "", "url": "", "snippet": ""}
            
        if "result__a" in classes:
            self._in_a = True
            if self._current_result is not None:
                raw_href = attrs_dict.get("href", "")
                if "uddg=" in raw_href:
                    m = re.search(r"uddg=([^&]+)", raw_href)
                    if m:
                        self._current_result["url"] = urllib.parse.unquote(m.group(1))
                else:
                    self._current_result["url"] = raw_href

        if "result__snippet" in classes:
            self._in_snippet = True

    def handle_endtag(self, tag):
        if tag == "a":
            self._in_a = False
            self._in_snippet = False

    def handle_data(self, data):
        if self._current_result is not None:
            if self._in_a:
                self._current_result["title"] += data
            elif self._in_snippet:
                self._current_result["snippet"] += data

    def close(self):
        super().close()
        if self._current_result and self._current_result.get("title"):
            self.results.append(self._current_result)

class ResearchService:
    """
    Internet Research & Web Knowledge Retrieval Engine.
    Uses pure standard library parsing to avoid external dependencies.
    """
    def __init__(self):
        self.headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        }

    async def search_web(self, query: str, max_results: int = 5) -> List[Dict[str, str]]:
        """Searches DuckDuckGo and parses top organic results using HTMLParser."""
        results = []
        try:
            encoded_query = urllib.parse.quote_plus(query)
            url = f"https://html.duckduckgo.com/html/?q={encoded_query}"

            async with httpx.AsyncClient(timeout=10.0, follow_redirects=True) as client:
                res = await client.get(url, headers=self.headers)
                if res.status_code == 200:
                    parser = DuckDuckGoHTMLParser()
                    parser.feed(res.text)
                    parser.close()
                    for r in parser.results:
                        title = r.get("title", "").strip()
                        raw_url = r.get("url", "").strip()
                        snippet = r.get("snippet", "").strip()
                        if title and raw_url and not raw_url.startswith("/"):
                            results.append({
                                "title": title,
                                "url": raw_url,
                                "snippet": snippet
                            })
                            if len(results) >= max_results:
                                break
        except Exception as e:
            logger.warning(f"DuckDuckGo search error: {e}")

        # Fallback instant answer if HTML parsing didn't yield
        if not results:
            try:
                api_url = f"https://api.duckduckgo.com/?q={urllib.parse.quote_plus(query)}&format=json"
                async with httpx.AsyncClient(timeout=8.0) as client:
                    res = await client.get(api_url, headers=self.headers)
                    if res.status_code == 200:
                        data = res.json()
                        abstract = data.get("AbstractText", "")
                        abstract_url = data.get("AbstractURL", "")
                        source = data.get("AbstractSource", "Web Source")
                        if abstract:
                            results.append({
                                "title": f"{source}: {query}",
                                "url": abstract_url or "https://duckduckgo.com",
                                "snippet": abstract
                            })
            except Exception as e:
                logger.warning(f"Instant Answer fallback error: {e}")

        return results

    async def conduct_research(self, query: str, api_key: Optional[str] = None) -> Dict:
        """Runs web retrieval followed by synthesized answer from free AI model."""
        sources = await self.search_web(query, max_results=5)

        context_blocks = []
        for i, s in enumerate(sources, 1):
            context_blocks.append(f"Source [{i}]: {s['title']}\nURL: {s['url']}\nSnippet: {s['snippet']}")

        context_text = "\n\n".join(context_blocks) if context_blocks else "No live web results returned for this query."

        prompt = f"""You are DOKI's Research Intelligence Module.
User asked: "{query}"

Live Web Search Findings:
{context_text}

Instructions:
1. Provide a concise, comprehensive research synthesis answering the user's question directly.
2. Cite sources using [1], [2], etc., corresponding to the search findings above.
3. Explicitly distinguish current facts verified from web sources vs general AI reasoning.
4. Keep the tone crisp, objective, and executive."""

        ai_resp = await ai_service.complete_chat(
            messages=[{"role": "user", "content": prompt}],
            api_key=api_key
        )

        content = ai_resp.get("content", "") if "content" in ai_resp else ai_resp.get("error", "Failed to generate research synthesis.")
        model_used = ai_resp.get("model", "openrouter/free")

        return {
            "query": query,
            "synthesis": content,
            "sources": sources,
            "model_used": model_used
        }

research_service = ResearchService()
