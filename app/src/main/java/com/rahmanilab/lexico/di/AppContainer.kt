package com.rahmanilab.lexico.di

import android.content.Context
import com.rahmanilab.lexico.data.DatabaseSeeder
import com.rahmanilab.lexico.data.backup.ImportExportRepository
import com.rahmanilab.lexico.data.local.LexicoDatabase
import com.rahmanilab.lexico.data.preferences.SettingsRepository
import com.rahmanilab.lexico.data.repository.CardRepository
import com.rahmanilab.lexico.data.repository.DeckRepository
import com.rahmanilab.lexico.data.repository.ReviewRepository
import com.rahmanilab.lexico.data.repository.StatsRepository
import com.rahmanilab.lexico.domain.scheduler.FsrsScheduler
import com.rahmanilab.lexico.domain.scheduler.Scheduler
import com.rahmanilab.lexico.domain.scheduler.Sm2Scheduler
import com.rahmanilab.lexico.tts.PronunciationManager
import com.rahmanilab.lexico.work.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency-injection container. A single instance lives on [com.rahmanilab.lexico.LexicoApplication]
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

    val reviewRepository: ReviewRepository by lazy {
        ReviewRepository(database, sm2Scheduler, fsrsScheduler, settingsRepository)
    }

    val statsRepository: StatsRepository by lazy { StatsRepository(database) }

    val importExportRepository: ImportExportRepository by lazy {
        ImportExportRepository(database, deckRepository, cardRepository)
    }

    val pronunciationManager: PronunciationManager by lazy { PronunciationManager(appContext) }

    val databaseSeeder: DatabaseSeeder by lazy {
        DatabaseSeeder(deckRepository, cardRepository, settingsRepository)
    }

    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(appContext) }
}
