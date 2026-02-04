package com.pulluptrainer

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class ProgressManager(context: Context) {
    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("pullup_progress", Context.MODE_PRIVATE)
    
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

        // Берем текущий интервал между тренировками из настроек (1/2/3 дня)
        val settingsManager = SettingsManager(appContext)
        val workoutIntervalDays = settingsManager.getWorkoutIntervalDays()
        val interval = if (workoutIntervalDays <= 0) 1 else workoutIntervalDays
        
        // Рассчитываем дату начала так, чтобы выбранный день был сегодня
        // Если выбран общий день N, то дата начала = сегодня - (N-1)*interval дней
        val daysToSubtract = (globalDayNumber - 1) * interval
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
     * Хранение подтягиваний по дням: ключи вида daily_pullups_yyyyMMdd
     */
    private fun getTodayKey(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return String.format("%04d%02d%02d", year, month, day)
    }

    fun addPullupsForToday(count: Int) {
        if (count <= 0) return
        val key = "daily_pullups_${getTodayKey()}"
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + count).apply()
    }

    /**
     * Возвращает статистику по дням: список (timestamp, количество), отсортированный по дате.
     * timestamp — полночь соответствующего дня.
     */
    fun getDailyPullups(): List<Pair<Long, Int>> {
        val all = prefs.all
        val result = mutableListOf<Pair<Long, Int>>()
        val cal = Calendar.getInstance()

        for ((key, value) in all) {
            if (key.startsWith("daily_pullups_") && value is Int) {
                val datePart = key.removePrefix("daily_pullups_")
                if (datePart.length == 8) {
                    val year = datePart.substring(0, 4).toIntOrNull() ?: continue
                    val month = datePart.substring(4, 6).toIntOrNull() ?: continue
                    val day = datePart.substring(6, 8).toIntOrNull() ?: continue
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, month - 1)
                    cal.set(Calendar.DAY_OF_MONTH, day)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    result.add(cal.timeInMillis to value)
                }
            }
        }

        return result.sortedBy { it.first }
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
        val editor = prefs.edit()
        editor.putInt("personal_record", 0)
        editor.putInt("total_pullups", 0)
        editor.putInt("completed_sets", 0)
        editor.remove("active_level")
        editor.remove("active_day")

        // Удаляем дневную статистику
        for ((key, _) in prefs.all) {
            if (key.startsWith("daily_pullups_")) {
                editor.remove(key)
            }
        }
        editor.apply()
        
        // Сбрасываем все отметки о выполненных тренировках
        for (level in WorkoutData.levels) {
            for (day in level.days) {
                unmarkWorkoutCompleted(level.levelNumber, day.dayNumber)
            }
        }
    }

    /**
     * Заполняет статистику тестовыми данными для отладки.
     * Несколько тренировок в разные дни + записи по дням.
     */
    fun fillTestDataForDebug() {
        val editor = prefs.edit()

        // Базовая статистика
        editor.putInt("personal_record", 20)
        editor.putInt("total_pullups", 150)
        editor.putInt("completed_sets", 40)

        // Несколько выполненных тренировок (уровень 1, дни 1–3)
        editor.putBoolean("completed_1_1", true)
        editor.putBoolean("completed_1_2", true)
        editor.putBoolean("completed_1_3", true)

        // Текущий прогресс – пусть будет уровень 1, день 4
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        editor.putInt(KEY_CURRENT_LEVEL, 1)
        editor.putInt(KEY_CURRENT_DAY, 4)
        editor.putLong(KEY_START_DATE, cal.timeInMillis)

        // Подтягивания по дням: последние несколько дней
        fun keyForOffset(offsetDays: Int): String {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, offsetDays)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH) + 1
            val day = c.get(Calendar.DAY_OF_MONTH)
            return "daily_pullups_${String.format("%04d%02d%02d", year, month, day)}"
        }

        editor.putInt(keyForOffset(-6), 10) // 6 дней назад
        editor.putInt(keyForOffset(-4), 12) // 4 дня назад
        editor.putInt(keyForOffset(-2), 14) // 2 дня назад
        editor.putInt(keyForOffset(-1), 15) // вчера
        editor.putInt(keyForOffset(0), 16)  // сегодня

        editor.apply()
    }
}
