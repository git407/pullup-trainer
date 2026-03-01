package com.pulluptrainer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "workout_reminder_channel"
        private const val CHANNEL_NAME = "Напоминания о тренировках"
        const val NOTIFICATION_ID = 1
        private const val WORK_NAME = "workout_reminder"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Напоминания о тренировках подтягиваний"
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Склонение «подтягивание» по правилам русского языка (не зависит от локали устройства).
     */
    private fun getPullupsFormRu(n: Int, context: Context): String {
        val res = context.resources
        return when {
            n % 10 == 1 && n % 100 != 11 -> res.getString(R.string.workout_reminder_pullups_one, n)
            n % 10 in 2..4 && n % 100 !in 12..14 -> res.getString(R.string.workout_reminder_pullups_few, n)
            else -> res.getString(R.string.workout_reminder_pullups_many, n)
        }
    }

    /**
     * Показывает уведомление о дне тренировки (вызывается из Worker или Receiver).
     */
    fun showWorkoutReminder(context: Context, level: Int, day: Int) {
        val workoutDay = WorkoutData.levels.getOrNull(level - 1)?.days?.getOrNull(day - 1) ?: return
        val totalPullups = workoutDay.sets.sum()
        val pullupsStr = getPullupsFormRu(totalPullups, context)
        val notificationText = context.getString(R.string.workout_reminder_day_pullups, day, pullupsStr)
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.workout_reminder_title))
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /**
     * Планирует напоминание на указанную дату и время через WorkManager (стабильно без точных будильников).
     */
    fun scheduleNotification(timestamp: Long, level: Int, day: Int, hour: Int = 10, minute: Int = 0) {
        if (timestamp == 0L) return
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val triggerTime = calendar.timeInMillis
        val delayMs = triggerTime - System.currentTimeMillis()
        if (delayMs <= 0) return
        val request = OneTimeWorkRequestBuilder<WorkoutReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(
                WorkoutReminderWorker.KEY_LEVEL to level,
                WorkoutReminderWorker.KEY_DAY to day
            ))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Отменяет запланированное напоминание.
     */
    fun cancelNotification() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
