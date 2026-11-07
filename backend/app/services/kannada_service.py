import re
from typing import Dict, Any, Optional, Tuple

class KannadaIntelligenceService:
    """
    Advanced Kannada Language Intelligence Service for DOKI.
    Handles:
    - Native Kannada Script (UTF-8 \u0C80-\u0CFF)
    - Kannada transliterated into English letters (Kanglish)
    - North Karnataka & Standard Kannada dialect normalization
    - Kannada intent extraction for Windows & Android device commands
    - Automatic language detection & code-switching
    """

    # Common Kanglish keywords and verbal inflections
    KANGLISH_PATTERNS = [
        r"\b(madu|maadu|madi|maadi|madoke|madodu|madbeda)\b",           # do / make
        r"\b(eshtu|yeshtu|yestu)\b",                                     # how much
        r"\b(hege|yaake|yake|yaaru|yaru|elli|illi|alli)\b",             # how/why/who/where/here/there
        r"\b(yenu|enu|yen|en)\b",                                       # what
        r"\b(beku|beda|agalla|aagalla|aagutte|agutte)\b",               # want / don't want / possible
        r"\b(namaskara|namaste|hegiddeera|hegiddira|hyangadeeri)\b",   # greetings
        r"\b(helu|heli|torisu|kanisu)\b",                               # tell / show
        r"\b(nanna|nanna|nan|nam)\b",                                   # my / our
        r"\b(laptop|phone|mobile)nalli\b",                              # in laptop / in phone
        r"\b(tege|mucche|muchu|jaasti|kammi)\b",                        # open / close / more / less
    ]

    # North Karnataka dialectal mappings to standard Kannada / English intents
    NORTH_KARNATAKA_VOCAB = {
        "ಹ್ಯಾಂಗದೀರಿ": "hegiddeera",      # How are you (NK: Hyangadeeri)
        "ಹ್ಯಾಂಗ": "hege",               # How (NK: Hyang)
        "ಏನ್ ಮಾಡಾತೀರಿ": "enu maaduttiddira", # What are you doing (NK: En maadaateeri)
        "ಬರ್ತೇನಿ": "baruttene",         # I will come (NK: Barteeni)
        "ಹುಡುಕಿ ಕೊಡು": "search maadu",  # Search for this
        "ಹೌದ್ರಿ": "yes",                # Yes sir (NK: Houdri)
        "ಯಾವಾಗ್ರೀ": "yaavaaga",         # When sir (NK: Yaavaagri)
        "ಖರೆ": "true",                  # Real / truth (NK: Khare)
    }

    # Intent regex patterns for Kannada and Kanglish
    COMMAND_PATTERNS = [
        # 1. Open Application (Windows / Android)
        {
            "intent": "open_app",
            "regex": [
                r"(?:ನನ್ನ\s+)?(?:ಲ್ಯಾಪ್ಟಾಪ್\s+ನಲ್ಲಿ\s+|ಲ್ಯಾಪ್ಟಾಪ್\s+)?([a-zA-Z0-9\s]+?)\s*(?:ಓಪನ್\s*ಮಾಡು|ತೆಗೆ|ಪ್ರಾರಂಭಿಸು)",
                r"(?:nanna\s+)?(?:laptop(?:nalli)?\s+)?([a-zA-Z0-9\s]+?)\s*(?:open\s*maadu|open\s*madu|tege)",
                r"(?:ನನ್ನ\s+)?(?:ಫೋನ್\s+ನಲ್ಲಿ\s+|ಮೊಬೈಲ್\s+ನಲ್ಲಿ\s+)?([a-zA-Z0-9\s]+?)\s*(?:ಓಪನ್\s*ಮಾಡು|ತೆಗೆ)",
                r"(?:nanna\s+)?(?:phone(?:nalli)?|mobile(?:nalli)?)\s+([a-zA-Z0-9\s]+?)\s*(?:open\s*maadu|open\s*madu)",
            ],
            "default_target": "laptop"
        },
        # 2. Check RAM / Memory
        {
            "intent": "check_ram",
            "regex": [
                r"(?:ಲ್ಯಾಪ್ಟಾಪ್|ಸಿಸ್ಟಮ್|ಕಂಪ್ಯೂಟರ್)?\s*(?:ram|ರ್ಯಾಮ್|ಮೆಮೊರಿ)\s*(?:ಎಷ್ಟು\s*(?:ಬಳಕೆಯಾಗಿದೆ|ಖಾಲಿಯಿದೆ|ಇದೆ)|ತಿಳಿಸು)",
                r"(?:laptop|system|pc)?\s*(?:ram|memory)\s*(?:eshtu|yeshtu)\s*(?:balakeyaagide|ide|usage)",
            ],
            "target": "laptop"
        },
        # 3. Check Battery
        {
            "intent": "check_battery",
            "regex": [
                r"(?:ಲ್ಯಾಪ್ಟಾಪ್|ಫೋನ್|ಮೊಬೈಲ್)?\s*(?:ಬ್ಯಾಟರಿ|battery)\s*(?:ಎಷ್ಟು\s*ಇದೆ|ಶೇಕಡಾ|ಸ್ಥಿತಿ|ತಿಳಿಸು)",
                r"(?:laptop|phone|mobile)?\s*(?:battery|charge)\s*(?:eshtu|yeshtu)\s*(?:ide|percentage|status)",
            ],
            "target": "laptop"
        },
        # 4. Volume Control
        {
            "intent": "volume_control",
            "regex": [
                r"(?:ಧ್ವನಿ|ಶಬ್ದ|ವಾಲ್ಯೂಮ್|volume)\s*(?:ಹೆಚ್ಚು\s*ಮಾಡು|ಜಾಸ್ತಿ\s*ಮಾಡು|ಕಡಿಮೆ\s*ಮಾಡು|ಮ್ಯೂಟ್\s*ಮಾಡು)",
                r"(?:volume|sound|shabda)\s*(?:jaasti\s*maadu|kammi\s*maadu|mute\s*maadu|up|down)",
            ],
            "target": "laptop"
        },
        # 5. Lock Screen
        {
            "intent": "lock_screen",
            "regex": [
                r"(?:ಲ್ಯಾಪ್ಟಾಪ್|ಸ್ಕ್ರೀನ್|ಸ್ಕ್ರೀನ್\s*ಅನ್ನು)\s*(?:ಲಾಕ್\s*ಮಾಡು|ಮುಚ್ಚು)",
                r"(?:laptop|screen)\s*(?:lock\s*maadu|lock\s*madu)",
            ],
            "target": "laptop"
        },
        # 6. Internet Search
        {
            "intent": "web_search",
            "regex": [
                r"(.+?)\s*(?:ಬಗ್ಗೆ\s*(?:ಹುಡುಕು|ತಿಳಿಸು|ಮಾಹಿತಿ\s*ನೀಡು|ಸರ್ಚ್\s*ಮಾಡು))",
                r"(.+?)\s*(?:bagge\s*(?:huduku|search\s*maadu|search\s*madu|heli))",
            ],
            "target": "backend"
        }
    ]

    @classmethod
    def detect_language(cls, text: str) -> Tuple[str, float]:
        """
        Detects whether input is:
        - 'kn' (Native Kannada script: 0x0C80 to 0x0CFF)
        - 'kn-translit' (Kannada written in Latin script / Kanglish)
        - 'en' (English)
        Returns (language_code, confidence).
        """
        if not text or not text.strip():
            return "en", 1.0

        # Check for native Kannada Unicode block
        kannada_chars = len(re.findall(r"[\u0C80-\u0CFF]", text))
        total_alpha = len(re.findall(r"\w", text))

        if kannada_chars > 0 and total_alpha > 0:
            ratio = kannada_chars / total_alpha
            if ratio >= 0.25:
                return "kn", min(1.0, ratio + 0.3)

        # Check for Kanglish / Kannada transliteration
        text_lower = text.lower()
        matched_patterns = 0
        for pattern in cls.KANGLISH_PATTERNS:
            if re.search(pattern, text_lower):
                matched_patterns += 1

        if matched_patterns >= 1:
            confidence = min(0.95, 0.5 + (matched_patterns * 0.15))
            return "kn-translit", confidence

        return "en", 0.9

    @classmethod
    def parse_kannada_command(cls, user_text: str) -> Optional[Dict[str, Any]]:
        """
        Parses a Kannada or Kanglish voice/text command into an actionable DOKI device command.
        """
        text = user_text.strip()
        text_lower = text.lower()

        # Check for phone target hint
        is_phone_target = bool(
            re.search(r"(ಫೋನ್|ಮೊಬೈಲ್|phone|mobile)", text_lower)
        )

        for cmd in cls.COMMAND_PATTERNS:
            for pat in cmd["regex"]:
                match = re.search(pat, text, re.IGNORECASE)
                if match:
                    intent = cmd["intent"]
                    target = "android" if is_phone_target else cmd.get("target", cmd.get("default_target", "laptop"))

                    if intent == "open_app":
                        app_name = match.group(1).strip() if match.groups() else "app"
                        # Clean app name
                        app_name = re.sub(r"(ನನ್ನ|nanna|ಫೋನ್ನಲ್ಲಿ|phonenalli|laptopnalli|ಲ್ಯಾಪ್ಟಾಪ್ನಲ್ಲಿ)", "", app_name, flags=re.IGNORECASE).strip()
                        return {
                            "action": "open_app",
                            "target_device": target,
                            "params": {"app_name": app_name},
                            "matched_intent": "open_app",
                            "language": "kn"
                        }
                    elif intent == "check_ram":
                        return {
                            "action": "get_system_info",
                            "target_device": "laptop",
                            "params": {"metric": "ram"},
                            "matched_intent": "check_ram",
                            "language": "kn"
                        }
                    elif intent == "check_battery":
                        return {
                            "action": "get_system_info",
                            "target_device": target,
                            "params": {"metric": "battery"},
                            "matched_intent": "check_battery",
                            "language": "kn"
                        }
                    elif intent == "volume_control":
                        is_up = bool(re.search(r"(ಹೆಚ್ಚು|ಜಾಸ್ತಿ|jaasti|up)", text_lower))
                        is_mute = bool(re.search(r"(ಮ್ಯೂಟ್|mute)", text_lower))
                        return {
                            "action": "set_volume",
                            "target_device": "laptop",
                            "params": {"direction": "mute" if is_mute else ("up" if is_up else "down")},
                            "matched_intent": "volume_control",
                            "language": "kn"
                        }
                    elif intent == "lock_screen":
                        return {
                            "action": "lock_screen",
                            "target_device": "laptop",
                            "params": {},
                            "matched_intent": "lock_screen",
                            "language": "kn"
                        }
                    elif intent == "web_search":
                        query = match.group(1).strip() if match.groups() else text
                        return {
                            "action": "web_search",
                            "target_device": "backend",
                            "params": {"query": query},
                            "matched_intent": "web_search",
                            "language": "kn"
                        }

        return None

    @classmethod
    def get_kannada_guidelines(cls) -> str:
        """
        Returns rich instructions embedded in the AI system prompt for perfect Kannada responses.
        """
        return """
KANNADA & KANGLISH LANGUAGE RULES:
1. FIRST-CLASS KANNADA SUPPORT:
   - DOKI must treat Kannada as a primary native language.
   - If Deepak speaks or writes in Kannada (ಕನ್ನಡ), reply in natural, fluent Kannada script.
   - If Deepak writes in Kannada transliteration (Kanglish, e.g. "DOKI, Chrome open madu" or "nanna laptop ram eshtu ide?"), reply either in natural conversational Kannada script with English technical terms, or natural Kanglish.
   - Code-Switching: Gracefully handle mixed Kannada and English sentences (e.g. "DOKI, nanna resume summarize maadu", "YouTube open madu").
   - North Karnataka Vocabulary: Understand and honor North Karnataka phrases (e.g. "ಹ್ಯಾಂಗದೀರಿ" -> "ಚೆನ್ನಾಗಿದ್ದೇನೆ/ಆರಾಮಾಗಿದ್ದೇನೆ", "ಏನ್ ಮಾಡಾತೀರಿ", "ಖರೆ", "ಹೌದ್ರಿ").
   - Response Language Mirroring: ALWAYS match the language of Deepak's prompt. Do NOT switch to English if Deepak addressed you in Kannada!
"""

kannada_service = KannadaIntelligenceService()

