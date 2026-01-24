package com.pulluptrainer

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class ProgressManager(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("pullup_progress", Context.MODE_PRIVATE)
    
    private val KEY_CURRENT_LEVEL = "current_level"
    private val KEY_CURRENT_DAY = "current_day"
    private val KEY_START_DATE = "start_date" // timestamp в миллисекундах
    
    fun getCurrentLevel(): Int {
        return prefs.getInt(KEY_CURRENT_LEVEL, 1)
    }
    
    fun getCurrentDay(): Int {
        return prefs.getInt(KEY_CURRENT_DAY, 1)
    }
    
    fun getStartDate(): Long {
        return prefs.getLong(KEY_START_DATE, 0)
    }
    
    fun setCurrentProgress(level: Int, day: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // Рассчитываем общий номер дня с учетом всех предыдущих уровней
        val globalDayNumber = DateUtils.getGlobalDayNumber(level, day)
        
        // Рассчитываем дату начала так, чтобы выбранный день был сегодня
        // Если выбран общий день N, то дата начала = сегодня - (N-1)*2 дня
        val daysToSubtract = (globalDayNumber - 1) * 2
        calendar.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        
        prefs.edit()
            .putInt(KEY_CURRENT_LEVEL, level)
            .putInt(KEY_CURRENT_DAY, day)
            .putLong(KEY_START_DATE, calendar.timeInMillis)
            .apply()
    }
    
    fun isCurrentDay(level: Int, day: Int): Boolean {
        return getCurrentLevel() == level && getCurrentDay() == day
    }
    
    /**
     * Отмечает тренировку как выполненную
     */
    fun markWorkoutCompleted(level: Int, day: Int) {
        val key = "completed_${level}_${day}"
        prefs.edit().putBoolean(key, true).commit() // Используем commit() для синхронного сохранения
    }
    
    /**
     * Проверяет, выполнена ли тренировка
     */
    fun isWorkoutCompleted(level: Int, day: Int): Boolean {
        val key = "completed_${level}_${day}"
        return prefs.getBoolean(key, false)
    }
    
    /**
     * Снимает отметку о выполнении тренировки
     */
    fun unmarkWorkoutCompleted(level: Int, day: Int) {
        val key = "completed_${level}_${day}"
        prefs.edit().putBoolean(key, false).commit()
    }
    
    /**
     * Отмечает тренировку как начатую (активную)
     */
    fun setActiveWorkout(level: Int, day: Int) {
        prefs.edit()
            .putInt("active_level", level)
            .putInt("active_day", day)
            .apply()
    }
    
    /**
     * Проверяет, является ли тренировка активной (начатой)
     */
    fun isActiveWorkout(level: Int, day: Int): Boolean {
        val activeLevel = prefs.getInt("active_level", -1)
        val activeDay = prefs.getInt("active_day", -1)
        return activeLevel == level && activeDay == day
    }
    
    /**
     * Сбрасывает активную тренировку
     */
    fun clearActiveWorkout() {
        prefs.edit()
            .remove("active_level")
            .remove("active_day")
            .apply()
    }
    
    /**
     * Получает личный рекорд по подтягиваниям
     */
    fun getPersonalRecord(): Int {
        return prefs.getInt("personal_record", 0)
    }
    
    /**
     * Устанавливает новый личный рекорд
     */
    fun setPersonalRecord(record: Int) {
        prefs.edit().putInt("personal_record", record).apply()
    }
    
    /**
     * Сбрасывает личный рекорд
     */
    fun resetPersonalRecord() {
        prefs.edit().putInt("personal_record", 0).apply()
    }
    
    /**
     * Получает общее количество подтягиваний
     */
    fun getTotalPullups(): Int {
        return prefs.getInt("total_pullups", 0)
    }
    
    /**
     * Увеличивает общее количество подтягиваний на указанное значение
     */
    fun addTotalPullups(count: Int) {
        val current = getTotalPullups()
        prefs.edit().putInt("total_pullups", current + count).apply()
    }
    
    /**
     * Получает количество завершенных тренировок
     */
    fun getCompletedWorkoutsCount(): Int {
        var count = 0
        for (level in WorkoutData.levels) {
            for (day in level.days) {
                if (isWorkoutCompleted(level.levelNumber, day.dayNumber)) {
                    count++
                }
            }
        }
        return count
    }
    
    /**
     * Получает количество завершенных подходов
     */
    fun getCompletedSetsCount(): Int {
        return prefs.getInt("completed_sets", 0)
    }
    
    /**
     * Увеличивает количество завершенных подходов
     */
    fun addCompletedSet() {
        val current = getCompletedSetsCount()
        prefs.edit().putInt("completed_sets", current + 1).apply()
    }
    
    /**
     * Сбрасывает всю статистику
     */
    fun resetAllStatistics() {
        prefs.edit()
            .putInt("personal_record", 0)
            .putInt("total_pullups", 0)
            .putInt("completed_sets", 0)
            .apply()
        
        // Сбрасываем все отметки о выполненных тренировках
        for (level in WorkoutData.levels) {
            for (day in level.days) {
                unmarkWorkoutCompleted(level.levelNumber, day.dayNumber)
            }
        }
    }
}
