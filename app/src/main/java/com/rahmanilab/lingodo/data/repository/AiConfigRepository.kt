package com.rahmanilab.lingodo.data.repository

import com.rahmanilab.lingodo.data.preferences.SettingsRepository
import com.rahmanilab.lingodo.data.security.SecureKeyStore
import com.rahmanilab.lingodo.domain.model.AiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Ties the chosen [AiProvider] (a preference) to its encrypted API key (in [SecureKeyStore]). */
class AiConfigRepository(
    private val settings: SettingsRepository,
    private val secureKeyStore: SecureKeyStore
) {

    val provider: Flow<AiProvider> = settings.aiProviderName.map { AiProvider.fromName(it) }

    suspend fun currentProvider(): AiProvider = AiProvider.fromName(settings.currentAiProviderName())

    suspend fun setProvider(provider: AiProvider) = settings.setAiProviderName(provider.name)

    suspend fun setKey(provider: AiProvider, key: String) =
        secureKeyStore.putSecret(keyName(provider), key.trim())

    suspend fun clearKey(provider: AiProvider) = secureKeyStore.clearSecret(keyName(provider))

    suspend fun getKey(provider: AiProvider): String? = secureKeyStore.getSecret(keyName(provider))

    suspend fun hasKey(provider: AiProvider): Boolean = secureKeyStore.hasSecret(keyName(provider))

    suspend fun hasKeyForCurrent(): Boolean = hasKey(currentProvider())

    private fun keyName(provider: AiProvider) = "apikey_${provider.name}"
}
