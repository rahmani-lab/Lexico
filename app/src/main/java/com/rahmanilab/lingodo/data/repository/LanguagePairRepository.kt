package com.rahmanilab.lingodo.data.repository

import com.rahmanilab.lingodo.data.local.LingoDoDatabase
import com.rahmanilab.lingodo.data.local.entity.LanguagePairEntity
import com.rahmanilab.lingodo.data.preferences.SettingsRepository
import com.rahmanilab.lingodo.domain.model.Language
import kotlinx.coroutines.flow.Flow

/** A pair resolved into rich [Language] values for the UI/TTS. */
data class LanguagePair(
    val id: Long,
    val source: Language,
    val target: Language
) {
    companion object {
        fun from(entity: LanguagePairEntity) = LanguagePair(
            id = entity.id,
            source = Language.fromCode(entity.sourceCode),
            target = Language.fromCode(entity.targetCode)
        )
    }
}

class LanguagePairRepository(
    private val db: LingoDoDatabase,
    private val settingsRepository: SettingsRepository
) {

    private val pairDao get() = db.languagePairDao()
    private val deckDao get() = db.deckDao()

    fun observePairs(): Flow<List<LanguagePairEntity>> = pairDao.observeAll()

    val activePairId: Flow<Long> = settingsRepository.activePairId

    suspend fun getPair(id: Long): LanguagePairEntity? = pairDao.getById(id)

    suspend fun activePair(): LanguagePair {
        val id = settingsRepository.currentActivePairId()
        val entity = pairDao.getById(id) ?: pairDao.getAll().firstOrNull() ?: ensureDefault()
        return LanguagePair.from(entity)
    }

    /** BCP-47 tag of the active pair's target language, for defaulting a new card's TTS locale. */
    suspend fun activeTargetTtsTag(): String = activePair().target.ttsTag

    /** Create the app's default pair on first run (or after a wipe) and mark it active. */
    suspend fun ensureDefault(): LanguagePairEntity {
        pairDao.getAll().firstOrNull()?.let { return it }
        val id = pairDao.insert(
            LanguagePairEntity(sourceCode = "fa", targetCode = "en", createdAt = System.currentTimeMillis())
        ).takeIf { it != -1L } ?: pairDao.find("fa", "en")!!.id
        settingsRepository.setActivePairId(id)
        return pairDao.getById(id)!!
    }

    /** Create (or reuse) a pair and return its id. */
    suspend fun createPair(sourceCode: String, targetCode: String): Long {
        pairDao.find(sourceCode, targetCode)?.let { return it.id }
        val id = pairDao.insert(
            LanguagePairEntity(sourceCode = sourceCode, targetCode = targetCode, createdAt = System.currentTimeMillis())
        )
        return if (id != -1L) id else pairDao.find(sourceCode, targetCode)!!.id
    }

    suspend fun setActive(id: Long) = settingsRepository.setActivePairId(id)

    /**
     * Delete a pair. Refuses if it still has decks or is the only remaining pair, returning a short
     * reason instead; the caller surfaces it.
     */
    suspend fun deletePair(pair: LanguagePairEntity): String? {
        if (pairDao.count() <= 1) return "You need at least one language pair."
        if (deckDao.countByPair(pair.id) > 0) return "This pair still has decks. Move or delete them first."
        pairDao.delete(pair)
        if (settingsRepository.currentActivePairId() == pair.id) {
            pairDao.getAll().firstOrNull()?.let { settingsRepository.setActivePairId(it.id) }
        }
        return null
    }
}
