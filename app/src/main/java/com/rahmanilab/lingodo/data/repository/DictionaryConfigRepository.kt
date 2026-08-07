package com.rahmanilab.lingodo.data.repository

import com.rahmanilab.lingodo.data.preferences.SettingsRepository
import com.rahmanilab.lingodo.data.security.SecureKeyStore
import com.rahmanilab.lingodo.domain.model.DictionarySource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Ties the chosen [DictionarySource] (a preference) to its encrypted key, mirroring
 * [AiConfigRepository]. Keyless sources need no key and are always usable.
 */
class DictionaryConfigRepository(
    private val settings: SettingsRepository,
    private val secureKeyStore: SecureKeyStore
) {

    val source: Flow<DictionarySource> =
        settings.dictionarySourceName.map { DictionarySource.fromName(it) }

    suspend fun currentSource(): DictionarySource =
        DictionarySource.fromName(settings.currentDictionarySourceName())

    suspend fun setSource(source: DictionarySource) = settings.setDictionarySourceName(source.name)

    suspend fun setKey(source: DictionarySource, key: String) =
        secureKeyStore.putSecret(keyName(source), key.trim())

    suspend fun clearKey(source: DictionarySource) = secureKeyStore.clearSecret(keyName(source))

    suspend fun getKey(source: DictionarySource): String? = secureKeyStore.getSecret(keyName(source))

    suspend fun hasKey(source: DictionarySource): Boolean = secureKeyStore.hasSecret(keyName(source))

    /** A source is usable when it needs no key, or when the user has saved one. */
    suspend fun isUsable(source: DictionarySource): Boolean = source.keyless || hasKey(source)

    private fun keyName(source: DictionarySource) = "dictkey_${source.name}"
}
