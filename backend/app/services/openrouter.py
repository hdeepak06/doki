import json
import logging
from typing import AsyncGenerator, Dict, List, Optional
import httpx
from app.config import (
    settings,
    get_effective_api_key,
    get_active_model
)
from app.services.kannada_service import kannada_service

logger = logging.getLogger("doki.openrouter")

class OpenRouterService:
    def __init__(self):
        self.base_url = settings.OPENROUTER_BASE_URL
        self._cached_models: List[dict] = []
        self._cache_time: float = 0

    def _get_headers(self, api_key: Optional[str] = None) -> Dict[str, str]:
        key = api_key or get_effective_api_key()
        headers = {
            "HTTP-Referer": "https://github.com/hdeepak06/doki",
            "X-Title": "DOKI Personal AI Assistant",
            "Content-Type": "application/json"
        }
        if key:
            headers["Authorization"] = f"Bearer {key}"
        return headers

    def build_system_prompt(self, memories: Optional[List[dict]] = None, detected_lang: str = "en") -> str:
        memory_str = ""
        if memories:
            memory_items = [f"- [{m.get('category', 'general')}] {m.get('key')}: {m.get('value')}" for m in memories]
            memory_str = "\n".join(memory_items)
        else:
            memory_str = "No specific personal memories stored yet."

        kannada_rules = kannada_service.get_kannada_guidelines()

        lang_instruction = ""
        if detected_lang in ["kn", "kn-translit"]:
            lang_instruction = "CRITICAL: The user has spoken/written in Kannada or Kanglish. You MUST reply in fluent Kannada (ಕನ್ನಡ) or natural conversational Kanglish. Do NOT reply in pure English unless explicitly instructed."

        return f"""You are DOKI, a personal JARVIS-style AI assistant created specifically for your owner, Deepak.

CORE PERSONA & BEHAVIOR:
- Tone: Highly capable, sophisticated, loyal, crisp, and futuristic like JARVIS from Iron Man.
- Address your owner as "Deepak" or "Sir" when appropriate.
- Intelligence: You possess deep knowledge in programming, engineering, math, science, current affairs, and system automation.
- Multilingual Fluency: You natively understand and reply in English, Kannada (ಕನ್ನಡ), Hindi (हिन्दी), and mixed Hinglish/Kanglish.
- Honesty & Accuracy: You never fabricate facts or invent capabilities. If uncertain, state your uncertainty clearly.
- Device Awareness: You are the central brain for Deepak's Windows laptop and Android phone.

{kannada_rules}

{lang_instruction}

CURRENT MEMORIES & PREFERENCES:
{memory_str}

Speak directly, concisely, and execute requests efficiently. Let's get to work."""

    async def fetch_models(self, force_refresh: bool = False) -> List[dict]:
        """
        Dynamically fetches models from OpenRouter API and filters strictly for FREE models.
        Never exposes or enables paid models.
        """
        import time
        now = time.time()
        if self._cached_models and not force_refresh and (now - self._cache_time < 300):
            return self._cached_models

        free_models = []
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                res = await client.get(f"{self.base_url}/models", headers=self._get_headers())
                if res.status_code == 200:
                    data = res.json().get("data", [])
                    for m in data:
                        mid = m.get("id", "")
                        pricing = m.get("pricing", {})
                        prompt_price = float(pricing.get("prompt", 0) or 0)
                        completion_price = float(pricing.get("completion", 0) or 0)
                        
                        # A model is free if id is 'openrouter/free' or ends with ':free' or has 0 pricing
                        if mid == "openrouter/free" or mid.endswith(":free") or (prompt_price == 0 and completion_price == 0):
                            free_models.append({
                                "id": mid,
                                "name": m.get("name", mid),
                                "description": m.get("description", ""),
                                "context_length": m.get("context_length", 4096),
                                "pricing": pricing,
                                "is_free": True
                            })
        except Exception as e:
            logger.warning(f"Failed to fetch dynamic models from OpenRouter: {e}")

        # Always ensure standard free router and fallback models are present
        existing_ids = {m["id"] for m in free_models}
        for fb in settings.FALLBACK_MODELS:
            if fb not in existing_ids:
                clean_name = fb.split("/")[-1].replace(":free", "").replace("-", " ").title()
                free_models.append({
                    "id": fb,
                    "name": clean_name if fb != "openrouter/free" else "OpenRouter Free Dynamic Router",
                    "description": "Free community AI model provided by OpenRouter",
                    "context_length": 8192,
                    "is_free": True
                })

        self._cached_models = free_models
        self._cache_time = now
        return free_models

    async def stream_chat(
        self,
        messages: List[dict],
        model: Optional[str] = None,
        memories: Optional[List[dict]] = None
    ) -> AsyncGenerator[str, None]:
        """
        Streams response chunks with automatic fallback across free models.
        Handles rate limits, network failures and provider errors gracefully.
        """
        api_key = get_effective_api_key()
        if not api_key:
            yield json.dumps({
                "type": "error",
                "content": "OpenRouter API Key is missing. Please configure your key in DOKI Settings."
            }) + "\n"
            return

        requested_model = model or get_active_model()
        # Candidate models list starting with requested model followed by fallbacks
        candidate_models = [requested_model]
        for fb in settings.FALLBACK_MODELS:
            if fb not in candidate_models:
                candidate_models.append(fb)

        # Detect language of user query
        last_user_content = ""
        for m in reversed(messages):
            if m.get("role") == "user":
                last_user_content = m.get("content", "")
                break
        detected_lang, _ = kannada_service.detect_language(last_user_content)

        # Prepare messages payload with system prompt
        full_messages = [
            {"role": "system", "content": self.build_system_prompt(memories, detected_lang=detected_lang)}
        ]
        # Include conversation history
        for msg in messages:
            full_messages.append({"role": msg["role"], "content": msg["content"]})

        success = False
        last_error = ""

        for candidate in candidate_models:
            logger.info(f"Attempting OpenRouter completion with model: {candidate}")
            try:
                payload = {
                    "model": candidate,
                    "messages": full_messages,
                    "stream": True,
                    "temperature": 0.7,
                    "top_p": 0.9
                }

                async with httpx.AsyncClient(timeout=30.0) as client:
                    async with client.stream(
                        "POST",
                        f"{self.base_url}/chat/completions",
                        headers=self._get_headers(api_key),
                        json=payload
                    ) as response:
                        if response.status_code == 429:
                            last_error = f"Model {candidate} is rate-limited (HTTP 429). Trying fallback..."
                            logger.warning(last_error)
                            continue
                        elif response.status_code != 200:
                            err_body = await response.aread()
                            last_error = f"Model {candidate} error (HTTP {response.status_code}): {err_body.decode('utf-8', errors='ignore')}"
                            logger.warning(last_error)
                            continue

                        # Send model notification event
                        yield json.dumps({
                            "type": "start",
                            "model": candidate
                        }) + "\n"

                        async for line in response.aiter_lines():
                            if not line or not line.startswith("data: "):
                                continue
                            data_str = line[6:].strip()
                            if data_str == "[DONE]":
                                break
                            try:
                                chunk = json.loads(data_str)
                                choices = chunk.get("choices", [])
                                if choices:
                                    delta = choices[0].get("delta", {})
                                    content = delta.get("content", "")
                                    if content:
                                        yield json.dumps({
                                            "type": "chunk",
                                            "content": content
                                        }) + "\n"
                            except Exception:
                                continue

                        yield json.dumps({
                            "type": "end",
                            "model": candidate
                        }) + "\n"
                        success = True
                        break

            except httpx.RequestError as e:
                last_error = f"Network connection error to OpenRouter with {candidate}: {str(e)}"
                logger.warning(last_error)
                continue
            except Exception as e:
                last_error = f"Unexpected error with {candidate}: {str(e)}"
                logger.error(last_error)
                continue

        if not success:
            yield json.dumps({
                "type": "error",
                "content": f"Unable to generate response after trying available free models. Detail: {last_error}"
            }) + "\n"

    async def complete_chat(
        self,
        messages: List[dict],
        model: Optional[str] = None,
        memories: Optional[List[dict]] = None
    ) -> dict:
        """
        Synchronous non-streaming chat completion with automatic fallback.
        """
        api_key = get_effective_api_key()
        if not api_key:
            return {"error": "OpenRouter API Key is missing. Please set it in Settings."}

        requested_model = model or get_active_model()
        candidate_models = [requested_model] + [fb for fb in settings.FALLBACK_MODELS if fb != requested_model]

        full_messages = [
            {"role": "system", "content": self.build_system_prompt(memories)}
        ] + messages

        for candidate in candidate_models:
            try:
                async with httpx.AsyncClient(timeout=25.0) as client:
                    res = await client.post(
                        f"{self.base_url}/chat/completions",
                        headers=self._get_headers(api_key),
                        json={
                            "model": candidate,
                            "messages": full_messages,
                            "temperature": 0.7
                        }
                    )
                    if res.status_code == 200:
                        data = res.json()
                        choice = data.get("choices", [{}])[0]
                        return {
                            "role": "assistant",
                            "content": choice.get("message", {}).get("content", ""),
                            "model": candidate,
                            "usage": data.get("usage", {})
                        }
            except Exception as e:
                logger.warning(f"Fallback step failed for {candidate}: {e}")
                continue

        return {"error": "All available free models failed. Please verify OpenRouter connectivity."}

openrouter_service = OpenRouterService()

