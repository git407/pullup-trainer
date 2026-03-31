package com.pulluptrainer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class WorkoutReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Проверяем, включены ли уведомления
        val settingsManager = SettingsManager(context)
        if (!settingsManager.areNotificationsEnabled()) {
            // Уведомления отключены, не показываем уведомление
            return
        }
        
        val notificationId = intent.getIntExtra("notification_id", 1)
        val level = intent.getIntExtra("level", 1)
        val day = intent.getIntExtra("day", 1)
        
        // Получаем информацию о тренировке
        val workoutDay = WorkoutData.levels[level - 1].days[day - 1]
        val setsText = workoutDay.sets.joinToString(" - ")
        
        // Формируем текст уведомления
        val notificationText = "Сегодня $day день тренировки $setsText"
        
        // Создаем Intent для открытия MainActivity при нажатии на уведомление
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.workout_reminder_title))
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(notificationId, notification)
    }
}
