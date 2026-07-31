package com.rahmanilab.lingodo.data.autofill

import com.rahmanilab.lingodo.data.net.HttpJson
import com.rahmanilab.lingodo.domain.model.AiProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A minimal BYOK chat client. OpenAI, Groq and DeepSeek share the OpenAI-compatible
 * `/chat/completions` shape; Anthropic Claude uses `/v1/messages`; Google Gemini uses its native
 * `v1beta/models/{model}:generateContent` endpoint. Returns the model's raw text reply.
 *
 * A low [TEMPERATURE] is used across every provider to keep the structured JSON output stable.
 */
class LlmClient {

    // explicitNulls = false so Gemini's optional request fields aren't serialized as `null`.
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

    suspend fun chat(
        provider: AiProvider,
        model: String,
        apiKey: String,
        systemPrompt: String,
        userPrompt: String
    ): String = when (provider) {
        AiProvider.CLAUDE -> anthropic(provider, model, apiKey, systemPrompt, userPrompt)
        AiProvider.GEMINI -> gemini(provider, model, apiKey, systemPrompt, userPrompt)
        else -> openAiCompatible(provider, model, apiKey, systemPrompt, userPrompt)
    }

    private suspend fun openAiCompatible(
        provider: AiProvider,
        model: String,
        apiKey: String,
        systemPrompt: String,
        userPrompt: String
    ): String {
        val url = "${provider.defaultBaseUrl.trimEnd('/')}/chat/completions"
        val body = json.encodeToString(
            ChatRequest(
                model = model,
                messages = listOf(Message("system", systemPrompt), Message("user", userPrompt)),
                temperature = TEMPERATURE
            )
        )
        val response = HttpJson.postJson(url, body, mapOf("Authorization" to "Bearer $apiKey"))
        return json.decodeFromString<ChatResponse>(response)
            .choices.firstOrNull()?.message?.content.orEmpty()
    }

    private suspend fun anthropic(
        provider: AiProvider,
        model: String,
        apiKey: String,
        systemPrompt: String,
        userPrompt: String
    ): String {
        val url = "${provider.defaultBaseUrl.trimEnd('/')}/v1/messages"
        val body = json.encodeToString(
            AnthropicRequest(
                model = model,
                maxTokens = 1024,
                temperature = TEMPERATURE,
                system = systemPrompt,
                messages = listOf(Message("user", userPrompt))
            )
        )
        val response = HttpJson.postJson(
            url,
            body,
            mapOf("x-api-key" to apiKey, "anthropic-version" to "2023-06-01")
        )
        return json.decodeFromString<AnthropicResponse>(response)
            .content.firstOrNull { it.type == "text" }?.text.orEmpty()
    }

    /**
     * Google Gemini's native REST endpoint: POST v1beta/models/{model}:generateContent with the key
     * in the `x-goog-api-key` header (equivalent to the `?key=` query parameter, but kept out of the
     * URL). System text goes in `systemInstruction`; the reply is in candidates[0].content.parts.
     */
    private suspend fun gemini(
        provider: AiProvider,
        model: String,
        apiKey: String,
        systemPrompt: String,
        userPrompt: String
    ): String {
        val url = "${provider.defaultBaseUrl.trimEnd('/')}/models/$model:generateContent"
        val body = json.encodeToString(
            GeminiRequest(
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt))),
                contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(userPrompt)))),
                generationConfig = GeminiConfig(temperature = TEMPERATURE)
            )
        )
        val response = HttpJson.postJson(url, body, mapOf("x-goog-api-key" to apiKey))
        return json.decodeFromString<GeminiResponse>(response)
            .candidates.firstOrNull()?.content?.parts.orEmpty().joinToString("") { it.text }
    }

    // --- OpenAI-compatible shapes ---
    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Double
    )

    @Serializable
    private data class Message(val role: String, val content: String)

    @Serializable
    private data class ChatResponse(val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(val message: Message? = null)

    // --- Anthropic shapes ---
    @Serializable
    private data class AnthropicRequest(
        val model: String,
        @SerialName("max_tokens") val maxTokens: Int,
        val temperature: Double,
        val system: String,
        val messages: List<Message>
    )

    @Serializable
    private data class AnthropicResponse(val content: List<AnthropicContent> = emptyList())

    @Serializable
    private data class AnthropicContent(val type: String = "", val text: String = "")

    // --- Gemini native shapes ---
    @Serializable
    private data class GeminiRequest(
        val contents: List<GeminiContent>,
        val systemInstruction: GeminiContent? = null,
        val generationConfig: GeminiConfig? = null
    )

    @Serializable
    private data class GeminiContent(
        val parts: List<GeminiPart> = emptyList(),
        val role: String? = null
    )

    @Serializable
    private data class GeminiPart(val text: String = "")

    @Serializable
    private data class GeminiConfig(val temperature: Double)

    @Serializable
    private data class GeminiResponse(val candidates: List<GeminiCandidate> = emptyList())

    @Serializable
    private data class GeminiCandidate(val content: GeminiContent? = null)

    private companion object {
        /** Low temperature keeps structured JSON output stable and avoids garbled replies. */
        const val TEMPERATURE = 0.2
    }
}
