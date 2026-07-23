package com.rahmanilab.lexico.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Schedules the daily study reminder with WorkManager so it survives app and device restarts.
 *
 * A self-rescheduling one-time worker is used (instead of a periodic worker) so the notification
 * fires at a precise local time each day.
 */
class ReminderScheduler(private val context: Context) {

    fun schedule(hour: Int, minute: Int) {
        val now = ZonedDateTime.now()
        var next = now.withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        val delayMillis = Duration.between(now, next).toMillis()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "lexico_study_reminder"
        const val TAG = "study_reminder"
    }
}
