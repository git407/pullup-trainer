package com.pulluptrainer

import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class WorkoutReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settingsManager = SettingsManager(applicationContext)
        if (!settingsManager.areNotificationsEnabled()) return Result.success()
        val level = inputData.getInt(KEY_LEVEL, 1)
        val day = inputData.getInt(KEY_DAY, 1)
        NotificationHelper(applicationContext).showWorkoutReminder(applicationContext, level, day)
        return Result.success()
    }

    companion object {
        const val KEY_LEVEL = "level"
        const val KEY_DAY = "day"
    }
}
