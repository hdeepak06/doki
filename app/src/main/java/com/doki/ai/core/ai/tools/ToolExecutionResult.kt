package com.doki.ai.core.ai.tools

sealed class ToolExecutionResult {
    data class Success(
        val toolName: String,
        val output: String,
        val userFeedback: String
    ) : ToolExecutionResult()

    data class RequiresConfirmation(
        val toolName: String,
        val confirmationPrompt: String,
        val pendingArgs: Map<String, String>
    ) : ToolExecutionResult()

    data class Error(
        val toolName: String,
        val error: String,
        val explanation: String
    ) : ToolExecutionResult()
}

