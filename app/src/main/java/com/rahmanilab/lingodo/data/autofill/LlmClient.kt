package com.rahmanilab.lingodo.data.autofill

import com.rahmanilab.lingodo.data.net.HttpJson
import com.rahmanilab.lingodo.domain.model.AiProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A minimal BYOK chat client. OpenAI, Groq, DeepSeek and Gemini share the OpenAI-compatible
 * `/chat/completions` shape; Anthropic Claude uses its own `/v1/messages` endpoint. Returns the
 * model's raw text reply.
 */
class LlmClient {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun chat(
        provider: AiProvider,
        model: String,
        apiKey: String,
        systemPrompt: String,
        userPrompt: String
    ): String = if (provider == AiProvider.CLAUDE) {
        anthropic(provider, model, apiKey, systemPrompt, userPrompt)
    } else {
        openAiCompatible(provider, model, apiKey, systemPrompt, userPrompt)
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
                temperature = 0.4
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

    // --- OpenAI-compatible shapes ---
    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Double = 0.4
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
        val system: String,
        val messages: List<Message>
    )

    @Serializable
    private data class AnthropicResponse(val content: List<AnthropicContent> = emptyList())

    @Serializable
    private data class AnthropicContent(val type: String = "", val text: String = "")
}
