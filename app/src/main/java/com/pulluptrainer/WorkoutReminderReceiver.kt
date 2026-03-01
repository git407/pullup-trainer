package com.pulluptrainer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WorkoutReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settingsManager = SettingsManager(context)
        if (!settingsManager.areNotificationsEnabled()) return
        val level = intent.getIntExtra("level", 1)
        val day = intent.getIntExtra("day", 1)
        NotificationHelper(context).showWorkoutReminder(context, level, day)
    }
}
