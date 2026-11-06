package com.doki.ai.domain.model

import java.util.Locale

enum class Language(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val locale: Locale
) {
    ENGLISH("en-US", "English", "English", Locale.US),
    KANNADA("kn-IN", "Kannada", "ಕನ್ನಡ", Locale("kn", "IN")),
    HINDI("hi-IN", "Hindi", "हिन्दी", Locale("hi", "IN"));

    companion object {
        fun fromCode(code: String?): Language {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: entries.firstOrNull { code?.startsWith(it.locale.language, ignoreCase = true) == true }
                ?: ENGLISH
        }

        fun detectFromText(text: String): Language {
            // Check for Kannada Unicode block (0x0C80 - 0x0CFF)
            val hasKannada = text.any { it.code in 0x0C80..0x0CFF }
            if (hasKannada) return KANNADA

            // Check for Devanagari/Hindi Unicode block (0x0900 - 0x097F)
            val hasHindi = text.any { it.code in 0x0900..0x097F }
            if (hasHindi) return HINDI

            return ENGLISH
        }
    }
}

