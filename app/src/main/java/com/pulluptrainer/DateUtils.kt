package com.pulluptrainer

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateUtils {
    private val dateFormat = SimpleDateFormat("d MMM yyyy г.", Locale.forLanguageTag("ru"))
    
    /**
     * Рассчитывает общий номер дня с учетом всех предыдущих уровней
     * @param level номер уровня (1-based)
     * @param dayNumber номер дня в уровне (1-based)
     * @return общий номер дня от начала программы
     */
    fun getGlobalDayNumber(level: Int, dayNumber: Int): Int {
        var globalDay = 0
        // Суммируем дни из всех предыдущих уровней
        for (i in 0 until level - 1) {
            globalDay += WorkoutData.levels[i].days.size
        }
        // Добавляем текущий день
        globalDay += dayNumber
        return globalDay
    }
    
    /**
     * Рассчитывает дату тренировки для указанного уровня и дня
     * @param startDate дата начала тренировки (timestamp)
     * @param level номер уровня (1-based)
     * @param dayNumber номер дня в уровне (1-based)
     * @return дата тренировки (timestamp)
     */
    fun getWorkoutDate(startDate: Long, level: Int, dayNumber: Int, workoutIntervalDays: Int = 2): Long {
        if (startDate == 0L) return 0L
        
        val globalDayNumber = getGlobalDayNumber(level, dayNumber)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate
        // Интервал между тренировками (день 1 = день 0, день 2 = interval, день 3 = interval*2 и т.д.)
        val interval = if (workoutIntervalDays <= 0) 1 else workoutIntervalDays
        val daysToAdd = (globalDayNumber - 1) * interval
        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        return calendar.timeInMillis
    }
    
    /**
     * Рассчитывает дату тренировки для указанного дня (старый метод для обратной совместимости)
     * @param startDate дата начала тренировки (timestamp)
     * @param dayNumber общий номер дня (1-based)
     * @return дата тренировки (timestamp)
     */
    fun getWorkoutDateByGlobalDay(startDate: Long, dayNumber: Int, workoutIntervalDays: Int = 2): Long {
        if (startDate == 0L) return 0L
        
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate
        // Интервал между тренировками (день 1 = день 0, день 2 = interval, день 3 = interval*2 и т.д.)
        val interval = if (workoutIntervalDays <= 0) 1 else workoutIntervalDays
        val daysToAdd = (dayNumber - 1) * interval
        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        return calendar.timeInMillis
    }
    
    /**
     * Форматирует дату для отображения
     */
    fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return dateFormat.format(calendar.time)
    }
    
    /**
     * Проверяет, является ли дата сегодняшним днем
     */
    fun isToday(timestamp: Long): Boolean {
        if (timestamp == 0L) return false
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
               calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * Получает дату следующей тренировки
     */
    fun getNextWorkoutDate(startDate: Long, currentLevel: Int, currentDay: Int, workoutIntervalDays: Int = 2): Long {
        val globalDayNumber = getGlobalDayNumber(currentLevel, currentDay)
        return getWorkoutDateByGlobalDay(startDate, globalDayNumber + 1, workoutIntervalDays)
    }
    
    /**
     * Получает следующий уровень и день тренировки
     * @return Pair<level, day> или null, если тренировки закончились
     */
    fun getNextWorkoutLevelAndDay(currentLevel: Int, currentDay: Int): Pair<Int, Int>? {
        val currentLevelData = WorkoutData.levels[currentLevel - 1]
        
        if (currentDay < currentLevelData.days.size) {
            // Есть следующий день в этом уровне
            return Pair(currentLevel, currentDay + 1)
        } else if (currentLevel < WorkoutData.levels.size) {
            // Переходим на следующий уровень, день 1
            return Pair(currentLevel + 1, 1)
        }
        
        // Тренировки закончились
        return null
    }
}
