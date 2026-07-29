package com.rahmanilab.lingodo.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rahmanilab.lingodo.LingoDoApplication
import com.rahmanilab.lingodo.MainActivity
import com.rahmanilab.lingodo.R

/**
 * Shows the daily "time to study" notification (when there is anything to study) and then
 * reschedules itself for the next day.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as LingoDoApplication).container
        val settings = container.settingsRepository.current()

        if (settings.reminderEnabled) {
            val studyable = container.database.cardScheduleDao()
                .countStudyable(System.currentTimeMillis())
            if (studyable > 0) {
                showNotification(studyable)
            }
            // Reschedule for tomorrow at the configured time.
            container.reminderScheduler.schedule(settings.reminderHour, settings.reminderMinute)
        }
        return Result.success()
    }

    private fun showNotification(studyableCount: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createChannel()

        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        val text = applicationContext.resources.getQuantityString(
            R.plurals.reminder_message, studyableCount, studyableCount
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(applicationContext.getString(R.string.reminder_title))
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    applicationContext.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = applicationContext.getString(R.string.reminder_channel_description)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "study_reminders"
        const val NOTIFICATION_ID = 1001
    }
}
