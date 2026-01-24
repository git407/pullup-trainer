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
}
