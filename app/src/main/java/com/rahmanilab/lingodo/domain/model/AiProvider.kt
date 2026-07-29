package com.rahmanilab.lingodo.domain.model

/**
 * A Bring-Your-Own-Key AI provider for the auto-fill engine. Requests go directly from the device
 * to the provider using the user's own key; LingoDo never proxies them through a server.
 */
enum class AiProvider(
    val displayName: String,
    val defaultModel: String,
    val defaultBaseUrl: String,
    val keyPortalUrl: String
) {
    GEMINI("Google Gemini", "gemini-1.5-flash", "https://generativelanguage.googleapis.com/v1beta/openai", "https://aistudio.google.com/app/apikey"),
    GROQ("Groq (free tier)", "llama-3.1-8b-instant", "https://api.groq.com/openai/v1", "https://console.groq.com/keys"),
    DEEPSEEK("DeepSeek", "deepseek-chat", "https://api.deepseek.com/v1", "https://platform.deepseek.com/api_keys"),
    OPENAI("OpenAI", "gpt-4o-mini", "https://api.openai.com/v1", "https://platform.openai.com/api-keys"),
    CLAUDE("Anthropic Claude", "claude-3-5-haiku-latest", "https://api.anthropic.com", "https://console.anthropic.com/settings/keys");

    companion object {
        fun fromName(name: String?): AiProvider = entries.firstOrNull { it.name == name } ?: GEMINI
    }
}
