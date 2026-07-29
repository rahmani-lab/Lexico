package com.rahmanilab.lingodo.data.autofill

import com.rahmanilab.lingodo.data.repository.AiConfigRepository
import com.rahmanilab.lingodo.domain.autofill.AutoFillEngine
import com.rahmanilab.lingodo.domain.autofill.AutoFillOutcome

/**
 * The auto-fill orchestrator. The full 3-tier network pipeline (keyless dictionary → free AI →
 * BYOK LLM) is wired to this seam, but live network fetching is intentionally disabled in this
 * build ("Base First" scope). It still reports its readiness so the UI and encrypted key storage
 * can be exercised end-to-end.
 */
class DefaultAutoFillEngine(
    private val aiConfig: AiConfigRepository
) : AutoFillEngine {

    override suspend fun enrich(word: String, sourceCode: String, targetCode: String): AutoFillOutcome {
        if (word.isBlank()) return AutoFillOutcome.Unavailable("Type a word first, then tap auto-fill.")

        // TODO(next round): Tier 1 dictionary + Tier 2/3 AI network calls populate AutoFillData here.
        return if (aiConfig.hasKeyForCurrent()) {
            AutoFillOutcome.Unavailable("Provider key saved. Live auto-fill turns on in the next update.")
        } else {
            AutoFillOutcome.Unavailable("Add an AI provider key in Settings → AI Auto-fill to enable auto-fill.")
        }
    }
}
