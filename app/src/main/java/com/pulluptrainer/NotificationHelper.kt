package com.pulluptrainer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "workout_reminder_channel"
        private const val CHANNEL_NAME = "Напоминания о тренировках"
        const val NOTIFICATION_ID = 1
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        // NotificationChannel доступен с API 26, minSdk = 26, проверка не нужна
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
     * Планирует уведомление на указанную дату
     */
    fun scheduleNotification(timestamp: Long, level: Int, day: Int, hour: Int = 10, minute: Int = 0) {
        if (timestamp == 0L) return
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WorkoutReminderReceiver::class.java).apply {
            putExtra("notification_id", NOTIFICATION_ID)
            putExtra("level", level)
            putExtra("day", day)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Устанавливаем будильник на заданное время (по умолчанию 10:00)
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        
        // setExactAndAllowWhileIdle доступен с API 23, minSdk = 26, проверка не нужна
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
    
    /**
     * Отменяет запланированное уведомление
     */
    fun cancelNotification() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WorkoutReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
