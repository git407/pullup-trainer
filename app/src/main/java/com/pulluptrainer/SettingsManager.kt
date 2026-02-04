package com.pulluptrainer

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("pullup_settings", Context.MODE_PRIVATE)
    
    private val KEY_SOUND_ENABLED = "sound_enabled"
    private val KEY_ASSISTANT = "assistant"
    private val KEY_THEME = "theme"
    private val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private val KEY_REST_INTERVAL_SECONDS = "rest_interval_seconds"
    private val KEY_WORKOUT_INTERVAL_DAYS = "workout_interval_days"
    private val KEY_NOTIFICATION_HOUR = "notification_hour"
    private val KEY_NOTIFICATION_MINUTE = "notification_minute"
    
    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }
    
    fun isSoundEnabled(): Boolean {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true) // По умолчанию включен
    }
    
    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }
    
    fun getSelectedAssistant(): String? {
        val assistant = prefs.getString(KEY_ASSISTANT, null)
        return if (assistant.isNullOrEmpty()) null else assistant
    }
    
    fun getSelectedAssistantWithDefault(): String? {
        // Если значение еще не было установлено, возвращаем "Chris" по умолчанию
        if (!prefs.contains(KEY_ASSISTANT)) {
            return "Chris"
        }
        val assistant = prefs.getString(KEY_ASSISTANT, null)
        return if (assistant.isNullOrEmpty()) null else assistant
    }
    
    fun hasAssistantBeenSet(): Boolean {
        return prefs.contains(KEY_ASSISTANT)
    }
    
    fun setSelectedAssistant(assistant: String?) {
        prefs.edit().putString(KEY_ASSISTANT, assistant).apply()
    }
    
    fun getTheme(): String {
        return prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
    }
    
    fun setTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
    }
    
    fun areNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true) // По умолчанию включены
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    /**
     * Интервал отдыха между подходами в секундах.
     * По умолчанию 60 секунд (1 минута) на первом запуске.
     */
    fun getRestIntervalSeconds(): Int {
        // Если значения ещё нет – вернётся 60
        return prefs.getInt(KEY_REST_INTERVAL_SECONDS, 60)
    }

    fun setRestIntervalSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_REST_INTERVAL_SECONDS, seconds).apply()
    }

    /**
     * Интервал между тренировками в днях.
     * По умолчанию 2 дня.
     */
    fun getWorkoutIntervalDays(): Int {
        return prefs.getInt(KEY_WORKOUT_INTERVAL_DAYS, 2)
    }

    fun setWorkoutIntervalDays(days: Int) {
        prefs.edit().putInt(KEY_WORKOUT_INTERVAL_DAYS, days).apply()
    }

    /**
     * Время уведомления о тренировке. По умолчанию 10:00.
     */
    fun getNotificationHour(): Int {
        return prefs.getInt(KEY_NOTIFICATION_HOUR, 10)
    }

    fun getNotificationMinute(): Int {
        return prefs.getInt(KEY_NOTIFICATION_MINUTE, 0)
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_NOTIFICATION_HOUR, hour)
            .putInt(KEY_NOTIFICATION_MINUTE, minute)
            .apply()
    }
}
