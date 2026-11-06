package com.doki.ai.core.voice

enum class VoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR;

    val isListening: Boolean get() = this == LISTENING
    val isSpeaking: Boolean get() = this == SPEAKING
    val isThinking: Boolean get() = this == THINKING
}

