package com.doki.ai.core.ai

import android.util.Log
import com.doki.ai.core.ai.tools.AndroidToolRegistry
import com.doki.ai.core.ai.tools.ToolExecutionResult
import com.doki.ai.domain.model.Language
import com.doki.ai.domain.model.Memory
import com.doki.ai.domain.model.Message
import com.doki.ai.domain.model.MessageRole
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GeminiAiService(
    private val toolRegistry: AndroidToolRegistry
) {
    private val tag = "GeminiAiService"

    /**
     * Builds dynamic system prompt integrating:
     * - DOKI assistant persona
     * - Multilingual instructions (English, Hindi, Kannada)
     * - Available Android Device Tools
     * - User's Long-Term Memories
     */
    private fun buildSystemInstruction(
        userMemories: List<Memory>,
        preferredLanguage: Language
    ): String {
        val memoryBlock = if (userMemories.isEmpty()) {
            "No personal memories recorded yet."
        } else {
            userMemories.joinToString("\n") { "- [${it.category}] ${it.key}: ${it.value}" }
        }

        val toolsCatalog = toolRegistry.toolDefinitions.joinToString("\n") { tool ->
            val params = tool.parameters.joinToString(", ") { "${it.name} (${it.type}): ${it.description}" }
            "- ${tool.name}: ${tool.description} | Params: [$params]"
        }

        return """
You are DOKI, a sophisticated, warm, and highly capable personal Android AI voice assistant.
Your wake phrase is "Hey DOKI".

CORE BEHAVIOR:
1. MULTILINGUAL INTELLIGENCE:
   - You natively support English, Hindi (हिन्दी), and Kannada (ಕನ್ನಡ).
   - If the user speaks or writes in Kannada, reply in natural, fluent Kannada script.
   - If the user speaks or writes in Hindi, reply in natural, fluent Devanagari Hindi script.
   - If the user speaks English or mixed Hinglish/Kanglish, reply naturally in the corresponding language.
   - Preferred user language is: ${preferredLanguage.displayName} (${preferredLanguage.code}).

2. CONCISE VOICE RESPONSES:
   - When spoken to, respond with concise, conversational, punchy sentences suitable for Text-To-Speech. Avoid unnecessary markdown tables or long bullet lists unless explicitly asked.

3. ANDROID DEVICE CONTROL TOOLS:
   You have access to native Android device tools:
$toolsCatalog

   If the user asks to perform an action (e.g. open an app, set an alarm, send an SMS, turn on the flashlight, check battery, navigate, save or retrieve memory), you MUST trigger the tool by outputting a JSON tool block at the START of your response:
   ```tool_call
   {"name": "tool_name", "arguments": {"param1": "value1"}}
   ```
   Followed by your brief spoken reply to the user.

4. PERSONAL MEMORY INTEGRATION:
   You remember user facts and preferences:
$memoryBlock

   Use these memories naturally when answering questions. If the user tells you to remember something new, call the `save_memory` tool!
""".trimIndent()
    }

    /**
     * Executes multi-turn prompt with Gemini, executing device tools if called.
     */
    suspend fun generateResponse(
        apiKey: String,
        modelName: String,
        history: List<Message>,
        userPrompt: String,
        userMemories: List<Memory>,
        preferredLanguage: Language
    ): Result<AiResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please enter your API key in Settings.")
            )
        }

        try {
            val detectedLanguage = Language.detectFromText(userPrompt)
            val systemInstruction = buildSystemInstruction(userMemories, preferredLanguage)

            val generativeModel = GenerativeModel(
                modelName = modelName.ifBlank { "gemini-1.5-flash" },
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.7f
                    topK = 40
                    topP = 0.95f
                },
                systemInstruction = content { text(systemInstruction) }
            )

            // Convert chat history
            val contents = mutableListOf<Content>()
            history.takeLast(10).forEach { msg ->
                val roleName = if (msg.role == MessageRole.USER) "user" else "model"
                contents.add(content(role = roleName) { text(msg.content) })
            }
            // Add current prompt
            contents.add(content(role = "user") { text(userPrompt) })

            val response = generativeModel.generateContent(*contents.toTypedArray())
            val rawText = response.text ?: ""

            // Check if tool call is embedded
            val (toolResult, cleanText) = parseAndExecuteTool(rawText)

            Result.success(
                AiResponse(
                    text = cleanText.ifBlank { toolResult?.let { if (it is ToolExecutionResult.Success) it.userFeedback else "" } ?: "Done." },
                    rawOutput = rawText,
                    detectedLanguage = detectedLanguage,
                    executedTool = toolResult
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "Gemini generation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Streams response chunks from Gemini for real-time speech and UI update.
     */
    fun streamResponse(
        apiKey: String,
        modelName: String,
        history: List<Message>,
        userPrompt: String,
        userMemories: List<Memory>,
        preferredLanguage: Language
    ): Flow<String> = flow {
        if (apiKey.isBlank()) {
            emit("Error: Gemini API key is missing. Please set it in Settings.")
            return@flow
        }
        val systemInstruction = buildSystemInstruction(userMemories, preferredLanguage)
        val generativeModel = GenerativeModel(
            modelName = modelName.ifBlank { "gemini-1.5-flash" },
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.7f
            },
            systemInstruction = content { text(systemInstruction) }
        )

        val contents = mutableListOf<Content>()
        history.takeLast(6).forEach { msg ->
            val roleName = if (msg.role == MessageRole.USER) "user" else "model"
            contents.add(content(role = roleName) { text(msg.content) })
        }
        contents.add(content(role = "user") { text(userPrompt) })

        val responseStream = generativeModel.generateContentStream(*contents.toTypedArray())
        val accumulated = StringBuilder()

        responseStream.collect { chunk ->
            chunk.text?.let { textChunk ->
                accumulated.append(textChunk)
                emit(textChunk)
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun parseAndExecuteTool(text: String): Pair<ToolExecutionResult?, String> {
        val toolRegex = Regex("```tool_call\\s*\\n?(\\{.*?\\})\\s*\\n?```", RegexOption.DOT_MATCHES_ALL)
        val match = toolRegex.find(text) ?: return Pair(null, text)

        val jsonString = match.groupValues[1]
        val cleanText = text.replace(match.value, "").trim()

        return try {
            val json = JSONObject(jsonString)
            val toolName = json.getString("name")
            val argsObj = json.optJSONObject("arguments") ?: JSONObject()
            val argsMap = mutableMapOf<String, Any?>()
            val keys = argsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                argsMap[key] = argsObj.get(key)
            }
            val result = toolRegistry.executeTool(toolName, argsMap)
            Pair(result, cleanText)
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse tool call: $jsonString", e)
            Pair(null, cleanText)
        }
    }
}

data class AiResponse(
    val text: String,
    val rawOutput: String,
    val detectedLanguage: Language,
    val executedTool: ToolExecutionResult? = null
)

