package com.rahmanilab.lingodo.di

import android.content.Context
import com.rahmanilab.lingodo.data.DatabaseSeeder
import com.rahmanilab.lingodo.data.autofill.DefaultAutoFillEngine
import com.rahmanilab.lingodo.data.backup.ImportExportRepository
import com.rahmanilab.lingodo.data.local.LexicoDatabase
import com.rahmanilab.lingodo.data.preferences.SettingsRepository
import com.rahmanilab.lingodo.data.repository.AiConfigRepository
import com.rahmanilab.lingodo.data.repository.CardRepository
import com.rahmanilab.lingodo.data.repository.DeckRepository
import com.rahmanilab.lingodo.data.repository.LanguagePairRepository
import com.rahmanilab.lingodo.data.repository.ReviewRepository
import com.rahmanilab.lingodo.data.repository.StatsRepository
import com.rahmanilab.lingodo.data.security.SecureKeyStore
import com.rahmanilab.lingodo.domain.autofill.AutoFillEngine
import com.rahmanilab.lingodo.domain.scheduler.FsrsScheduler
import com.rahmanilab.lingodo.domain.scheduler.Scheduler
import com.rahmanilab.lingodo.domain.scheduler.Sm2Scheduler
import com.rahmanilab.lingodo.tts.PronunciationManager
import com.rahmanilab.lingodo.work.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency-injection container. A single instance lives on [com.rahmanilab.lingodo.LexicoApplication]
 * and owns every app-wide singleton. Everything is created lazily so start-up stays cheap.
 *
 * This keeps the project free of an annotation-processing DI framework while still following the
 * recommended architecture (repositories in front of the data sources, a swappable [Scheduler],
 * ViewModels built from the container via factories).
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Long-lived scope for app-level work such as one-time seeding. */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: LexicoDatabase by lazy { LexicoDatabase.build(appContext) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val sm2Scheduler: Scheduler by lazy { Sm2Scheduler() }

    val fsrsScheduler: Scheduler by lazy { FsrsScheduler() }

    val deckRepository: DeckRepository by lazy { DeckRepository(database.deckDao()) }

    val cardRepository: CardRepository by lazy { CardRepository(database) }

    val languagePairRepository: LanguagePairRepository by lazy {
        LanguagePairRepository(database, settingsRepository)
    }

    val reviewRepository: ReviewRepository by lazy {
        ReviewRepository(database, sm2Scheduler, fsrsScheduler, settingsRepository)
    }

    val statsRepository: StatsRepository by lazy { StatsRepository(database) }

    val importExportRepository: ImportExportRepository by lazy {
        ImportExportRepository(database, deckRepository, cardRepository, settingsRepository)
    }

    val secureKeyStore: SecureKeyStore by lazy { SecureKeyStore(appContext) }

    val aiConfigRepository: AiConfigRepository by lazy {
        AiConfigRepository(settingsRepository, secureKeyStore)
    }

    val autoFillEngine: AutoFillEngine by lazy { DefaultAutoFillEngine(aiConfigRepository) }

    val pronunciationManager: PronunciationManager by lazy { PronunciationManager(appContext) }

    val databaseSeeder: DatabaseSeeder by lazy {
        DatabaseSeeder(deckRepository, cardRepository, settingsRepository, languagePairRepository)
    }

    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(appContext) }
}
