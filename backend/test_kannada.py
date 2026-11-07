import sys
from pathlib import Path

# Add backend to path
sys.path.insert(0, str(Path(__file__).parent))

from app.services.kannada_service import kannada_service
from app.api.chat import execute_direct_intent
from app.database import SessionLocal, init_db
from app.models.conversation import Conversation, Message

def run_tests():
    print("========================================================")
    print("      DOKI ADVANCED KANNADA INTELLIGENCE TEST SUITE      ")
    print("========================================================")

    # 1. Native Kannada Script Language Detection
    samples_native = [
        "ನನ್ನ ಲ್ಯಾಪ್ಟಾಪ್ RAM ಎಷ್ಟು ಬಳಕೆಯಾಗಿದೆ?",
        "ನನ್ನ ಫೋನ್ನಲ್ಲಿ WhatsApp ಓಪನ್ ಮಾಡು.",
        "ಇದನ್ನು ಕನ್ನಡದಲ್ಲಿ ವಿವರಿಸು.",
        "ಹ್ಯಾಂಗದೀರಿ ಸರ್?",
    ]
    print("\n[TEST 1] Native Kannada Script Detection:")
    for s in samples_native:
        lang, conf = kannada_service.detect_language(s)
        print(f" - '{s}' -> Lang: {lang} (Confidence: {conf:.2f})")
        assert lang == "kn", f"Expected 'kn', got {lang}"
    print("✓ Test 1 Passed!")

    # 2. Kannada Transliteration (Kanglish) Detection
    samples_kanglish = [
        "DOKI, Chrome open madu.",
        "nanna laptop ram eshtu ide?",
        "laptop volume jaasti maadu.",
        "namaskara DOKI, hegiddeera?",
    ]
    print("\n[TEST 2] Kanglish Transliteration Detection:")
    for s in samples_kanglish:
        lang, conf = kannada_service.detect_language(s)
        print(f" - '{s}' -> Lang: {lang} (Confidence: {conf:.2f})")
        assert lang == "kn-translit", f"Expected 'kn-translit', got {lang}"
    print("✓ Test 2 Passed!")

    # 3. Kannada Intent Parsing & Action Mapping
    print("\n[TEST 3] Intent Parsing & Device Command Extraction:")
    test_commands = [
        ("ನನ್ನ ಲ್ಯಾಪ್ಟಾಪ್ RAM ಎಷ್ಟು ಬಳಕೆಯಾಗಿದೆ?", "get_system_info", "laptop"),
        ("DOKI, Chrome open madu.", "open_app", "laptop"),
        ("ನನ್ನ ಫೋನ್ನಲ್ಲಿ WhatsApp ಓಪನ್ ಮಾಡು.", "open_app", "android"),
        ("nanna battery eshtu ide?", "get_system_info", "laptop"),
        ("ಲ್ಯಾಪ್ಟಾಪ್ ಸ್ಕ್ರೀನ್ ಲಾಕ್ ಮಾಡು", "lock_screen", "laptop"),
        ("volume jaasti maadu", "set_volume", "laptop"),
    ]

    for user_input, expected_action, expected_target in test_commands:
        parsed = kannada_service.parse_kannada_command(user_input)
        assert parsed is not None, f"Failed to parse: {user_input}"
        print(f" - Input: '{user_input}'")
        print(f"   -> Action: {parsed['action']} | Target: {parsed['target_device']} | Params: {parsed.get('params')}")
        assert parsed["action"] == expected_action, f"Expected action {expected_action}, got {parsed['action']}"
        assert parsed["target_device"] == expected_target, f"Expected target {expected_target}, got {parsed['target_device']}"
    print("✓ Test 3 Passed!")

    # 4. Direct Intent Execution in Kannada
    print("\n[TEST 4] Direct Intent Execution with Natural Kannada Output:")
    ram_cmd = kannada_service.parse_kannada_command("ನನ್ನ ಲ್ಯಾಪ್ಟಾಪ್ RAM ಎಷ್ಟು ಬಳಕೆಯಾಗಿದೆ?")
    ram_reply_kn = execute_direct_intent(ram_cmd, is_native_kannada=True)
    print(f" - Native Kannada Reply: '{ram_reply_kn}'")
    assert "ದೀಪಕ್ ಸರ್" in ram_reply_kn and "RAM" in ram_reply_kn

    ram_reply_en = execute_direct_intent(ram_cmd, is_native_kannada=False)
    print(f" - Kanglish Reply: '{ram_reply_en}'")
    assert "Deepak sir" in ram_reply_en

    # 5. Kannada Database Persistence (UTF-8 Verification)
    print("\n[TEST 5] SQLite UTF-8 Kannada Conversation Persistence:")
    init_db()
    with SessionLocal() as db:
        conv = Conversation(title="ಕನ್ನಡ ಸಂಭಾಷಣೆ")
        db.add(conv)
        db.commit()
        db.refresh(conv)

        msg1 = Message(conversation_id=conv.id, role="user", content="ನಮಸ್ಕಾರ DOKI, ಹೇಗಿದ್ದೀರಿ?")
        msg2 = Message(conversation_id=conv.id, role="assistant", content="ನಮಸ್ಕಾರ ದೀಪಕ್ ಸರ್! ನಾನು ಚೆನ್ನಾಗಿದ್ದೇನೆ. ನಿಮಗೆ ಹೇಗೆ ಸಹಾಯ ಮಾಡಲಿ?")
        db.add_all([msg1, msg2])
        db.commit()

        # Retrieve and verify
        saved = db.query(Message).filter(Message.conversation_id == conv.id).all()
        assert len(saved) == 2
        assert saved[0].content == "ನಮಸ್ಕಾರ DOKI, ಹೇಗಿದ್ದೀರಿ?"
        assert saved[1].content == "ನಮಸ್ಕಾರ ದೀಪಕ್ ಸರ್! ನಾನು ಚೆನ್ನಾಗಿದ್ದೇನೆ. ನಿಮಗೆ ಹೇಗೆ ಸಹಾಯ ಮಾಡಲಿ?"
        print(f" - Successfully retrieved stored Kannada messages from doki.db:")
        print(f"   User: {saved[0].content}")
        print(f"   DOKI: {saved[1].content}")

    print("✓ Test 5 Passed!")

    print("\n========================================================")
    print("  ALL 5 ADVANCED KANNADA INTELLIGENCE TESTS PASSED!     ")
    print("========================================================")

if __name__ == "__main__":
    run_tests()
