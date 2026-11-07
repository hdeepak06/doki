import json
import logging
import time
from typing import AsyncGenerator, Dict, List, Optional
import httpx
from app.config import (
    settings,
    get_effective_gemini_key,
    get_effective_openrouter_key,
    get_active_provider,
    get_active_model,
    is_free_only_mode,
    is_auto_fallback_enabled
)
from app.services.kannada_service import kannada_service

logger = logging.getLogger("doki.ai_service")

class AIService:
    """
    Centralized Multi-Provider AI Brain for DOKI JARVIS OS.
    
    PRIMARY PROVIDER: Google Gemini Free Tier (gemini-2.5-flash / gemini-2.0-flash / gemini-1.5-flash)
    SECONDARY PROVIDER: OpenRouter Free Models (openrouter/free)
    
    Features:
    - Automatic initialization of Gemini on startup & refresh
    - Real-time streaming (SSE)
    - Multilingual Kannada/Hindi/English system prompts
    - Automatic free fallback on HTTP 429 quota exhaustion, 503 server errors, model capacity
    - Strict Free-Only enforcement (zero paid models)
    """

    def __init__(self):
        self.gemini_base_url = "https://generativelanguage.googleapis.com/v1beta"
        self.openrouter_base_url = settings.OPENROUTER_BASE_URL
        self.last_provider_used = "gemini"
        self.last_model_used = settings.GEMINI_MODEL
        self.last_successful_request_time = 0

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
        elif detected_lang == "hi":
            lang_instruction = "The user has spoken/written in Hindi. Respond naturally in fluent Hindi (हिन्दी) or natural Hinglish."

        return f"""You are DOKI, a personal JARVIS-style AI operating assistant created specifically for your owner, Deepak.

CORE PERSONA & BEHAVIOR:
- Tone: Highly capable, sophisticated, loyal, crisp, and futuristic like JARVIS from Iron Man.
- Direct & Concise: Answer questions directly and execute requests with zero unnecessary fluff or AI clichés (never say "As an AI...").
- Personal Address: Address your owner as "Deepak" or "Sir".
- Greetings: When greeted ("good morning", "hi", "hello", "hey doki"), reply with signature JARVIS charm: "Good morning, Deepak. All systems are operational and running at peak performance. How may I assist you today?"
- Intelligence: Exceptional reasoning in programming (Python, React, TypeScript, C++, system design), physics, mathematics, and device automation.
- Multilingual Fluency: Full native comprehension and responses in English, Kannada (ಕನ್ನಡ), Hindi (हिन्दी), and Kanglish/Hinglish transliteration.
- Device Mastery: You act as the unified neural operating system for Deepak's Windows laptop and OPPO Android phone.
- Free Tier Awareness: Operating seamlessly on 100% zero-cost AI models.

{kannada_rules}

{lang_instruction}

CURRENT MEMORIES & PREFERENCES:
{memory_str}

Respond with intelligence, precision, and efficiency."""

    # =========================================================================
    # 1. GOOGLE GEMINI (PRIMARY AI BRAIN)
    # =========================================================================

    def _convert_to_gemini_payload(self, messages: List[dict], system_prompt: str) -> dict:
        """Converts OpenAI-style message format to Google Gemini API schema."""
        contents = []
        for msg in messages:
            role = "user" if msg.get("role") in ["user", "system"] else "model"
            text_content = msg.get("content", "")
            if text_content:
                contents.append({
                    "role": role,
                    "parts": [{"text": text_content}]
                })

        return {
            "contents": contents,
            "systemInstruction": {
                "parts": [{"text": system_prompt}]
            },
            "generationConfig": {
                "temperature": 0.7,
                "topP": 0.95
            }
        }

    async def _stream_gemini(
        self,
        messages: List[dict],
        system_prompt: str,
        api_key: str,
        model: str
    ) -> AsyncGenerator[str, None]:
        """Streams response directly from Google Gemini REST API using SSE."""
        payload = self._convert_to_gemini_payload(messages, system_prompt)
        url = f"{self.gemini_base_url}/models/{model}:streamGenerateContent?alt=sse&key={api_key}"
        headers = {
            "Content-Type": "application/json",
            "x-goog-api-key": api_key
        }

        async with httpx.AsyncClient(timeout=35.0) as client:
            async with client.stream(
                "POST",
                url,
                json=payload,
                headers=headers
            ) as response:
                if response.status_code == 429:
                    raise httpx.HTTPStatusError("Gemini quota or rate-limit exceeded (HTTP 429)", request=response.request, response=response)
                elif response.status_code in [401, 403]:
                    err_body = await response.aread()
                    raise PermissionError(f"Gemini API authentication failed (HTTP {response.status_code}): {err_body.decode('utf-8', errors='ignore')}")
                elif response.status_code != 200:
                    err_body = await response.aread()
                    raise httpx.HTTPStatusError(f"Gemini HTTP {response.status_code}: {err_body.decode('utf-8', errors='ignore')}", request=response.request, response=response)

                # Send initial provider & model announcement
                self.last_provider_used = "gemini"
                self.last_model_used = model
                self.last_successful_request_time = time.time()

                yield json.dumps({
                    "type": "start",
                    "provider": "gemini",
                    "model": model
                }) + "\n"

                buffer = ""
                async for line in response.aiter_lines():
                    if not line:
                        continue
                    if line.startswith("data: "):
                        data_str = line[6:].strip()
                        if not data_str:
                            continue
                        try:
                            chunk = json.loads(data_str)
                            candidates = chunk.get("candidates", [])
                            if candidates:
                                content = candidates[0].get("content", {})
                                parts = content.get("parts", [])
                                for p in parts:
                                    t = p.get("text", "")
                                    if t:
                                        yield json.dumps({
                                            "type": "chunk",
                                            "content": t
                                        }) + "\n"
                        except Exception:
                            continue

                yield json.dumps({
                    "type": "end",
                    "provider": "gemini",
                    "model": model
                }) + "\n"

    async def _complete_gemini(
        self,
        messages: List[dict],
        system_prompt: str,
        api_key: str,
        model: str
    ) -> dict:
        """Synchronous non-streaming Google Gemini completion."""
        payload = self._convert_to_gemini_payload(messages, system_prompt)
        url = f"{self.gemini_base_url}/models/{model}:generateContent?key={api_key}"
        headers = {
            "Content-Type": "application/json",
            "x-goog-api-key": api_key
        }

        async with httpx.AsyncClient(timeout=30.0) as client:
            res = await client.post(url, json=payload, headers=headers)
            if res.status_code == 200:
                data = res.json()
                candidates = data.get("candidates", [])
                if candidates:
                    parts = candidates[0].get("content", {}).get("parts", [])
                    full_text = "".join(p.get("text", "") for p in parts)
                    self.last_provider_used = "gemini"
                    self.last_model_used = model
                    self.last_successful_request_time = time.time()
                    return {
                        "role": "assistant",
                        "content": full_text,
                        "provider": "gemini",
                        "model": model
                    }
            elif res.status_code == 429:
                raise httpx.HTTPStatusError("Gemini quota exceeded", request=res.request, response=res)
            elif res.status_code in [401, 403]:
                raise PermissionError(f"Gemini API authentication failed: {res.text}")
            else:
                raise httpx.HTTPStatusError(f"Gemini error {res.status_code}", request=res.request, response=res)

        return {"error": "Failed to generate completion with Gemini"}

    # =========================================================================
    # 2. OPENROUTER FREE MODELS (SECONDARY FALLBACK)
    # =========================================================================

    def _get_openrouter_headers(self, key: Optional[str] = None) -> Dict[str, str]:
        headers = {
            "HTTP-Referer": "https://github.com/hdeepak06/doki",
            "X-Title": "DOKI Personal AI Assistant",
            "Content-Type": "application/json"
        }
        if key:
            headers["Authorization"] = f"Bearer {key.strip()}"
        return headers

    async def _stream_openrouter_fallback(
        self,
        full_messages: List[dict],
        openrouter_key: str
    ) -> AsyncGenerator[str, None]:
        """Streams response across verified free OpenRouter candidate models."""
        candidate_models = settings.OPENROUTER_FALLBACK_MODELS
        success = False
        last_err = ""

        for candidate in candidate_models:
            logger.info(f"DOKI Fallback: Attempting OpenRouter free model: {candidate}")
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
                        f"{self.openrouter_base_url}/chat/completions",
                        headers=self._get_openrouter_headers(openrouter_key),
                        json=payload
                    ) as response:
                        if response.status_code == 429:
                            last_err = f"OpenRouter {candidate} rate-limited (HTTP 429). Trying next free model..."
                            continue
                        elif response.status_code == 401:
                            last_err = "OpenRouter authentication failed (HTTP 401). Check OPENROUTER_API_KEY in Settings."
                            break
                        elif response.status_code != 200:
                            err_body = await response.aread()
                            last_err = f"OpenRouter {candidate} HTTP {response.status_code}: {err_body.decode('utf-8', errors='ignore')}"
                            continue

                        self.last_provider_used = "openrouter"
                        self.last_model_used = candidate
                        self.last_successful_request_time = time.time()

                        yield json.dumps({
                            "type": "start",
                            "provider": "openrouter",
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
                            "provider": "openrouter",
                            "model": candidate
                        }) + "\n"
                        success = True
                        break

            except Exception as e:
                last_err = f"Error with OpenRouter {candidate}: {str(e)}"
                continue

        if not success:
            yield json.dumps({
                "type": "error",
                "content": f"⚠️ Both Google Gemini and OpenRouter free fallbacks failed. Detail: {last_err}"
            }) + "\n"

    # =========================================================================
    # 3. UNIFIED CHAT STREAM (GEMINI FIRST -> OPENROUTER FALLBACK)
    # =========================================================================

    async def stream_chat(
        self,
        messages: List[dict],
        model: Optional[str] = None,
        memories: Optional[List[dict]] = None,
        api_key: Optional[str] = None
    ) -> AsyncGenerator[str, None]:
        """
        Unified streaming interface.
        Always prioritizes Google Gemini.
        Automatically falls back to OpenRouter free models if Gemini hits 429 quota or fails.
        """
        is_openrouter_key = bool(api_key and (api_key.startswith("sk-or-") or api_key.startswith("sk-")))
        gemini_key = (api_key if api_key and not is_openrouter_key else None) or get_effective_gemini_key()
        openrouter_key = (api_key if is_openrouter_key else None) or get_effective_openrouter_key()

        # Language detection
        last_user_content = ""
        for m in reversed(messages):
            if m.get("role") == "user":
                last_user_content = m.get("content", "")
                break
        detected_lang, _ = kannada_service.detect_language(last_user_content)
        system_prompt = self.build_system_prompt(memories, detected_lang=detected_lang)

        # 1. ATTEMPT PRIMARY: GOOGLE GEMINI
        gemini_models = [model] if (model and "gemini" in model.lower()) else settings.GEMINI_FALLBACK_MODELS
        gemini_attempted = False
        gemini_error = ""

        if gemini_key:
            gemini_attempted = True
            for g_model in gemini_models:
                try:
                    logger.info(f"DOKI AI: Attempting Primary Google Gemini ({g_model})...")
                    async for chunk in self._stream_gemini(messages, system_prompt, gemini_key, g_model):
                        yield chunk
                    return  # Success!
                except PermissionError as pe:
                    gemini_error = f"Gemini Authentication Error: {str(pe)}"
                    logger.error(gemini_error)
                    break  # Stop trying other Gemini models if key is invalid
                except (httpx.HTTPStatusError, httpx.RequestError, Exception) as ge:
                    gemini_error = str(ge)
                    logger.warning(f"Gemini {g_model} failed: {ge}. Checking fallback...")
                    continue

        # 2. ATTEMPT SECONDARY: OPENROUTER FREE FALLBACK
        auto_fallback = is_auto_fallback_enabled()
        if auto_fallback and openrouter_key:
            logger.info("DOKI AI: Triggering Secondary OpenRouter Free Fallback...")
            full_msgs = [{"role": "system", "content": system_prompt}] + messages
            async for chunk in self._stream_openrouter_fallback(full_msgs, openrouter_key):
                yield chunk
            return

        # 3. IF NO KEYS OR ALL PROVIDERS UNAVAILABLE
        if not gemini_key and not openrouter_key:
            yield json.dumps({
                "type": "error",
                "content": "⚠️ Google Gemini API Key is missing.\n\nPlease configure your free Gemini key in DOKI Settings (⚙️ icon) or add GEMINI_API_KEY to your Vercel Environment Variables (Free key at https://aistudio.google.com/app/apikey)."
            }) + "\n"
        else:
            yield json.dumps({
                "type": "error",
                "content": f"⚠️ AI request failed. Gemini Error: {gemini_error or 'Key not configured'}. OpenRouter Fallback: {'Key missing' if not openrouter_key else 'Exhausted'}."
            }) + "\n"

    # =========================================================================
    # 4. UNIFIED COMPLETION (NON-STREAMING)
    # =========================================================================

    async def complete_chat(
        self,
        messages: List[dict],
        model: Optional[str] = None,
        memories: Optional[List[dict]] = None,
        api_key: Optional[str] = None
    ) -> dict:
        is_openrouter_key = bool(api_key and (api_key.startswith("sk-or-") or api_key.startswith("sk-")))
        gemini_key = (api_key if api_key and not is_openrouter_key else None) or get_effective_gemini_key()
        openrouter_key = (api_key if is_openrouter_key else None) or get_effective_openrouter_key()

        last_user_content = ""
        for m in reversed(messages):
            if m.get("role") == "user":
                last_user_content = m.get("content", "")
                break
        detected_lang, _ = kannada_service.detect_language(last_user_content)
        system_prompt = self.build_system_prompt(memories, detected_lang=detected_lang)

        # Primary: Gemini
        if gemini_key:
            for g_model in settings.GEMINI_FALLBACK_MODELS:
                try:
                    res = await self._complete_gemini(messages, system_prompt, gemini_key, g_model)
                    if "content" in res:
                        return res
                except Exception as e:
                    logger.warning(f"Gemini {g_model} completion failed: {e}")
                    continue

        # Secondary: OpenRouter
        if openrouter_key:
            full_msgs = [{"role": "system", "content": system_prompt}] + messages
            for candidate in settings.OPENROUTER_FALLBACK_MODELS:
                try:
                    async with httpx.AsyncClient(timeout=25.0) as client:
                        res = await client.post(
                            f"{self.openrouter_base_url}/chat/completions",
                            headers=self._get_openrouter_headers(openrouter_key),
                            json={"model": candidate, "messages": full_msgs, "temperature": 0.7}
                        )
                        if res.status_code == 200:
                            data = res.json()
                            content = data.get("choices", [{}])[0].get("message", {}).get("content", "")
                            self.last_provider_used = "openrouter"
                            self.last_model_used = candidate
                            return {"role": "assistant", "content": content, "provider": "openrouter", "model": candidate}
                except Exception:
                    continue

        return {"error": "All AI providers failed. Please check your Gemini or OpenRouter configuration."}

    # =========================================================================
    # 5. CONNECTIVITY & HEALTH CHECK
    # =========================================================================

    async def check_connectivity(self, gemini_key: Optional[str] = None, openrouter_key: Optional[str] = None) -> dict:
        g_key = gemini_key or get_effective_gemini_key()
        o_key = openrouter_key or get_effective_openrouter_key()

        gemini_status = "NOT_CONFIGURED"
        if g_key:
            try:
                headers = {"x-goog-api-key": g_key}
                async with httpx.AsyncClient(timeout=8.0) as client:
                    res = await client.get(f"{self.gemini_base_url}/models?key={g_key}", headers=headers)
                    if res.status_code == 200:
                        gemini_status = "CONNECTED"
                    elif res.status_code in [401, 403]:
                        gemini_status = "INVALID_KEY"
                    else:
                        gemini_status = f"ERROR_{res.status_code}"
            except Exception:
                gemini_status = "OFFLINE"

        openrouter_status = "NOT_CONFIGURED"
        if o_key:
            try:
                async with httpx.AsyncClient(timeout=8.0) as client:
                    res = await client.get(f"{self.openrouter_base_url}/auth/key", headers=self._get_openrouter_headers(o_key))
                    if res.status_code == 200:
                        openrouter_status = "CONNECTED"
                    elif res.status_code == 401:
                        openrouter_status = "INVALID_KEY"
                    else:
                        openrouter_status = f"ERROR_{res.status_code}"
            except Exception:
                openrouter_status = "OFFLINE"

        active_p = "gemini" if gemini_status == "CONNECTED" else ("openrouter" if openrouter_status == "CONNECTED" else "none")

        return {
            "backend_status": "ONLINE",
            "primary_provider": "gemini",
            "gemini": {
                "configured": bool(g_key),
                "status": gemini_status,
                "model": settings.GEMINI_MODEL,
                "free_tier": True
            },
            "openrouter_fallback": {
                "configured": bool(o_key),
                "status": openrouter_status,
                "model": settings.OPENROUTER_MODEL,
                "free_tier": True
            },
            "active_provider": active_p,
            "last_successful_model": self.last_model_used,
            "last_successful_request_time": self.last_successful_request_time
        }

ai_service = AIService()
openrouter_service = ai_service  # Alias for backward compatibility
