"""
DOKI System Utilities and Helpers
"""
import re
import unicodedata

def sanitize_string(text: str) -> str:
    """Sanitizes user input strings, stripping control characters."""
    if not text:
        return ""
    return "".join(ch for ch in text if unicodedata.category(ch)[0] != "C").strip()

def is_kannada_text(text: str) -> bool:
    """Quick check if a string contains Kannada Unicode characters."""
    return bool(re.search(r"[\u0C80-\u0CFF]", text))
