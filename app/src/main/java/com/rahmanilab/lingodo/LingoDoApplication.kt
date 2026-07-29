package com.rahmanilab.lingodo

import android.app.Application
import com.rahmanilab.lingodo.di.AppContainer
import kotlinx.coroutines.launch

class LingoDoApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        container.applicationScope.launch {
            // Populate the starter deck on first launch.
            container.databaseSeeder.seedIfNeeded()

            // Re-arm the study reminder (WorkManager may have been cleared by an update/reinstall).
            val settings = container.settingsRepository.current()
            if (settings.reminderEnabled) {
                container.reminderScheduler.schedule(settings.reminderHour, settings.reminderMinute)
            }
        }
    }
}
