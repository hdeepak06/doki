package com.doki.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageTest {

    @Test
    fun testKannadaDetection() {
        val kannadaText = "ಹಲೋ ಡೋಕಿ, ಹೇಗಿದ್ದೀಯಾ?"
        val detected = Language.detectFromText(kannadaText)
        assertEquals(Language.KANNADA, detected)
    }

    @Test
    fun testHindiDetection() {
        val hindiText = "नमस्ते डोकी, आज का मौसम कैसा है?"
        val detected = Language.detectFromText(hindiText)
        assertEquals(Language.HINDI, detected)
    }

    @Test
    fun testEnglishDetection() {
        val englishText = "Hey Doki, what is the battery percentage?"
        val detected = Language.detectFromText(englishText)
        assertEquals(Language.ENGLISH, detected)
    }

    @Test
    fun testLanguageFromCode() {
        assertEquals(Language.KANNADA, Language.fromCode("kn-IN"))
        assertEquals(Language.HINDI, Language.fromCode("hi-IN"))
        assertEquals(Language.ENGLISH, Language.fromCode("en-US"))
        assertEquals(Language.ENGLISH, Language.fromCode("invalid_code"))
    }
}

