package com.rahmanilab.lingodo.data.net

import java.io.IOException

/**
 * Maps raw network/API failures into short, friendly, user-facing messages so technical payloads
 * (HTTP 402, credit_balance_exhausted, HTTP 429, stack traces…) never reach the UI.
 */
object AiErrors {

    fun friendlyMessage(error: Throwable?): String {
        if (error == null) return GENERAL
        val chain = error.causes().toList()

        // Raw HTTP failures carry the provider's status + payload — never show them directly.
        val http = chain.filterIsInstance<HttpJson.HttpException>().firstOrNull()
        if (http != null) {
            val body = http.bodySnippet.lowercase()
            return when {
                http.code == 402 || "credit_balance" in body || "insufficient balance" in body ||
                    "insufficient_balance" in body || "billing" in body -> UNAVAILABLE
                http.code == 429 || "insufficient_quota" in body || "quota" in body ||
                    "rate limit" in body || "rate_limit" in body -> BUSY
                http.code == 404 || "not_found" in body || "not found" in body -> MODEL_ISSUE
                else -> GENERAL
            }
        }

        // No HTTP error left, so any remaining IO failure is a connectivity problem.
        if (chain.any { it is IOException }) return NO_INTERNET

        // Preserve the app's own intentional, already-friendly signals (e.g. "Add an AI key…");
        // anything else (parsing errors, etc.) falls back to the generic message.
        val appMessage = chain.firstOrNull { it is IllegalStateException }?.message
        return appMessage?.takeIf { it.isNotBlank() } ?: GENERAL
    }

    private fun Throwable.causes(): Sequence<Throwable> = generateSequence(this) { it.cause }

    const val UNAVAILABLE =
        "The AI service is currently unavailable. Please try again later or switch to a different model in Settings."
    const val BUSY = "High volume of requests right now. Please wait a moment and try again."
    const val MODEL_ISSUE = "AI model configuration issue. Please select another model in Settings."
    const val NO_INTERNET = "No internet connection. Please check your network and try again."
    const val GENERAL = "Something went wrong while fetching data. Please try again."
}
