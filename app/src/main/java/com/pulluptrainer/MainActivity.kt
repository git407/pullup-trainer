package com.pulluptrainer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressManager: ProgressManager
    private lateinit var adapter: WorkoutAdapter
    private lateinit var notificationHelper: NotificationHelper
    
    private fun applyTheme() {
        val settingsManager = SettingsManager(this)
        val theme = settingsManager.getTheme()
        val themeResId = when (theme) {
            SettingsManager.THEME_LIGHT -> R.style.Theme_PullUpTrainer_Light
            SettingsManager.THEME_DARK -> R.style.Theme_PullUpTrainer_Dark
            else -> R.style.Theme_PullUpTrainer // Системная
        }
        setTheme(themeResId)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Применяем тему перед setContentView
        applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Инициализируем текущую тему
        val settingsManager = SettingsManager(this)
        currentTheme = settingsManager.getTheme()
        
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        
        progressManager = ProgressManager(this)
        notificationHelper = NotificationHelper(this)
        
        // Запрашиваем разрешения для уведомлений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
        
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Если это первый запуск (нет сохраненного дня), устанавливаем день 1 уровня 1
        val isFirstLaunch = progressManager.getStartDate() == 0L
        if (isFirstLaunch) {
            progressManager.setCurrentProgress(1, 1)
        }
        
        adapter = WorkoutAdapter(WorkoutData.levels, progressManager) { level, day ->
            showWorkoutMenu(level, day)
        }
        recyclerView.adapter = adapter
        
        // Планируем уведомление при запуске приложения (если уведомления включены)
        if (settingsManager.areNotificationsEnabled()) {
            val startDate = progressManager.getStartDate()
            if (startDate != 0L) {
                val currentLevel = progressManager.getCurrentLevel()
                val currentDay = progressManager.getCurrentDay()
                val nextWorkout = DateUtils.getNextWorkoutLevelAndDay(currentLevel, currentDay)
                if (nextWorkout != null) {
                    val nextWorkoutDate = DateUtils.getNextWorkoutDate(startDate, currentLevel, currentDay)
                    notificationHelper.scheduleNotification(nextWorkoutDate, nextWorkout.first, nextWorkout.second)
                }
            }
        } else {
            // Если уведомления отключены, отменяем запланированные
            notificationHelper.cancelNotification()
        }
        
        // Прокручиваем к текущему дню при запуске
        recyclerView.post {
            scrollToCurrentDay()
        }
    }
    
    private var currentTheme: String? = null
    
    override fun onResume() {
        super.onResume()
        
        // Проверяем, изменилась ли тема
        val settingsManager = SettingsManager(this)
        val newTheme = settingsManager.getTheme()
        if (currentTheme != null && currentTheme != newTheme) {
            // Тема изменилась, перезапускаем активность
            recreate()
            return
        }
        currentTheme = newTheme
        
        // Проверяем настройку уведомлений и обновляем их при необходимости
        if (settingsManager.areNotificationsEnabled()) {
            // Если уведомления включены, планируем их заново
            val startDate = progressManager.getStartDate()
            if (startDate != 0L) {
                val currentLevel = progressManager.getCurrentLevel()
                val currentDay = progressManager.getCurrentDay()
                val nextWorkout = DateUtils.getNextWorkoutLevelAndDay(currentLevel, currentDay)
                if (nextWorkout != null) {
                    val nextWorkoutDate = DateUtils.getNextWorkoutDate(startDate, currentLevel, currentDay)
                    notificationHelper.cancelNotification()
                    notificationHelper.scheduleNotification(nextWorkoutDate, nextWorkout.first, nextWorkout.second)
                }
            }
        } else {
            // Если уведомления отключены, отменяем запланированные
            notificationHelper.cancelNotification()
        }
        
        // Обновляем список при возврате на экран (например, после завершения тренировки)
        adapter?.notifyDataSetChanged()
    }
    
    
    private fun scrollToCurrentDay() {
        val currentLevel = progressManager.getCurrentLevel()
        val currentDay = progressManager.getCurrentDay()
        
        // Находим позицию текущего дня в списке
        var position = 0
        for (level in WorkoutData.levels) {
            if (level.levelNumber == currentLevel) {
                // Позиция заголовка уровня
                position++
                // Ищем день в этом уровне
                for (day in level.days) {
                    if (day.dayNumber == currentDay) {
                        // Нашли! Прокручиваем к этой позиции
                        (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
                        return
                    }
                    position++
                }
                break
            } else {
                // Пропускаем этот уровень (заголовок + все дни)
                position += level.days.size + 1
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menu_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showWorkoutMenu(level: Int, day: Int) {
        val workoutDay = WorkoutData.levels[level - 1].days[day - 1]
        val isCurrent = progressManager.isCurrentDay(level, day)
        val isCompleted = progressManager.isWorkoutCompleted(level, day)
        
        val completeWorkoutText = if (isCompleted) {
            getString(R.string.uncomplete_workout)
        } else {
            getString(R.string.complete_workout)
        }
        
        val options = arrayOf(
            getString(R.string.start_workout),
            getString(R.string.set_workout),
            completeWorkoutText
        )
        
        AlertDialog.Builder(this)
            .setTitle("День ${String.format("%02d", day)} - Уровень $level")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Начать тренировку
                        // Сначала устанавливаем эту тренировку как текущую
                        progressManager.setCurrentProgress(level, day)
                        // Планируем уведомление на следующий день тренировки (если уведомления включены)
                        val settingsManager = SettingsManager(this)
                        if (settingsManager.areNotificationsEnabled()) {
                            val startDate = progressManager.getStartDate()
                            val currentLevel = progressManager.getCurrentLevel()
                            val currentDay = progressManager.getCurrentDay()
                            val nextWorkout = DateUtils.getNextWorkoutLevelAndDay(currentLevel, currentDay)
                            if (nextWorkout != null) {
                                val nextWorkoutDate = DateUtils.getNextWorkoutDate(startDate, currentLevel, currentDay)
                                notificationHelper.cancelNotification()
                                notificationHelper.scheduleNotification(nextWorkoutDate, nextWorkout.first, nextWorkout.second)
                            }
                        } else {
                            notificationHelper.cancelNotification()
                        }
                        // Отмечаем эту тренировку как активную
                        progressManager.setActiveWorkout(level, day)
                        adapter.notifyDataSetChanged()
                        
                        val intent = Intent(this, WorkoutActivity::class.java).apply {
                            putExtra("level", level)
                            putExtra("day", day)
                        }
                        startActivity(intent)
                    }
                     1 -> {
                        // Установить тренировку
                        progressManager.setCurrentProgress(level, day)
                        // Планируем уведомление на следующий день тренировки (если уведомления включены)
                        val settingsManager = SettingsManager(this)
                        if (settingsManager.areNotificationsEnabled()) {
                            val startDate = progressManager.getStartDate()
                            val currentLevel = progressManager.getCurrentLevel()
                            val currentDay = progressManager.getCurrentDay()
                            val nextWorkout = DateUtils.getNextWorkoutLevelAndDay(currentLevel, currentDay)
                            if (nextWorkout != null) {
                                val nextWorkoutDate = DateUtils.getNextWorkoutDate(startDate, currentLevel, currentDay)
                                notificationHelper.cancelNotification()
                                notificationHelper.scheduleNotification(nextWorkoutDate, nextWorkout.first, nextWorkout.second)
                            }
                        } else {
                            notificationHelper.cancelNotification()
                        }
                        adapter.notifyDataSetChanged()
                    }
                    2 -> {
                        // Отметить выполненной/невыполненной
                        if (isCompleted) {
                            // Снимаем отметку о выполнении
                            progressManager.unmarkWorkoutCompleted(level, day)
                            adapter.notifyDataSetChanged()
                        } else {
                            // Просто отмечаем как выполненную, не меняя текущий прогресс и не пересчитывая даты
                            progressManager.markWorkoutCompleted(level, day)
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}

class WorkoutAdapter(
    private val levels: List<WorkoutLevel>,
    private val progressManager: ProgressManager,
    private val onDayClick: (Int, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    companion object {
        private const val TYPE_LEVEL_HEADER = 0
        private const val TYPE_DAY = 1
    }
    
    override fun getItemViewType(position: Int): Int {
        var currentPos = 0
        for (level in levels) {
            if (currentPos == position) return TYPE_LEVEL_HEADER
            currentPos++
            if (position < currentPos + level.days.size) return TYPE_DAY
            currentPos += level.days.size
        }
        return TYPE_LEVEL_HEADER
    }
    
    override fun getItemCount(): Int {
        return levels.sumOf { it.days.size + 1 } // +1 for level header
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_LEVEL_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_level_header, parent, false)
                LevelHeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_day, parent, false)
                DayViewHolder(view)
            }
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var currentPos = 0
        for (level in levels) {
            if (currentPos == position) {
                (holder as LevelHeaderViewHolder).bind(level)
                return
            }
            currentPos++
            
            for (day in level.days) {
                if (currentPos == position) {
                    val isCurrent = progressManager.isCurrentDay(level.levelNumber, day.dayNumber)
                    val isActive = progressManager.isActiveWorkout(level.levelNumber, day.dayNumber)
                    val startDate = progressManager.getStartDate()
                    (holder as DayViewHolder).bind(day, level.levelNumber, isCurrent, isActive, startDate, progressManager) {
                        onDayClick(level.levelNumber, day.dayNumber)
                    }
                    return
                }
                currentPos++
            }
        }
    }
    
    class LevelHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val levelText: TextView = itemView.findViewById(R.id.levelText)
        private val daysCountText: TextView = itemView.findViewById(R.id.daysCountText)
        
        fun bind(level: WorkoutLevel) {
            levelText.text = "⭐ Уровень ${level.levelNumber}"
            val daysCount = level.days.size
            daysCountText.text = "$daysCount дней(дня)"
        }
    }
    
    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayText: TextView = itemView.findViewById(R.id.dayText)
        private val dateText: TextView = itemView.findViewById(R.id.dateText)
        private val setsText: TextView = itemView.findViewById(R.id.setsText)
        private val currentDayIcon: ImageView = itemView.findViewById(R.id.currentDayIcon)
        private val completedIcon: ImageView = itemView.findViewById(R.id.completedIcon)
        
        fun bind(
            day: WorkoutDay,
            level: Int,
            isCurrent: Boolean,
            isActive: Boolean,
            startDate: Long,
            progressManager: ProgressManager,
            onClick: () -> Unit
        ) {
            dayText.text = "День ${String.format("%02d", day.dayNumber)}"
            setsText.text = day.getSetsString()
            
            // Рассчитываем и отображаем дату
            val workoutDate = DateUtils.getWorkoutDate(startDate, level, day.dayNumber)
            if (workoutDate != 0L) {
                if (DateUtils.isToday(workoutDate) && isCurrent) {
                    dateText.text = itemView.context.getString(R.string.today)
                    dateText.visibility = View.VISIBLE
                } else {
                    dateText.text = DateUtils.formatDate(workoutDate)
                    dateText.visibility = View.VISIBLE
                }
            } else {
                dateText.visibility = View.GONE
            }
            
            // Проверяем, выполнена ли тренировка
            val isCompleted = progressManager.isWorkoutCompleted(level, day.dayNumber)
            
            // Показываем галочку для выполненных тренировок перед текстом подходов
            if (isCompleted) {
                completedIcon.visibility = View.VISIBLE
            } else {
                completedIcon.visibility = View.GONE
            }
            
            // Получаем цвета из темы
            val typedValue = android.util.TypedValue()
            val theme = itemView.context.theme
            
            theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            val textColorPrimary = if (typedValue.resourceId != 0) {
                ContextCompat.getColor(itemView.context, typedValue.resourceId)
            } else {
                typedValue.data
            }
            
            theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
            val textColorSecondary = if (typedValue.resourceId != 0) {
                ContextCompat.getColor(itemView.context, typedValue.resourceId)
            } else {
                typedValue.data
            }
            
            // Выделяем текущий день или активную тренировку серым фоном и показываем стрелку
            if (isCurrent || isActive) {
                itemView.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.current_day_bg)
                )
                // Для выделенного элемента используем черный текст на сером фоне
                val textColor = ContextCompat.getColor(itemView.context, android.R.color.black)
                dayText.setTextColor(textColor)
                dateText.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
                setsText.setTextColor(textColor)
                currentDayIcon.visibility = View.VISIBLE
                currentDayIcon.setColorFilter(textColor)
            } else {
                itemView.setBackgroundColor(
                    itemView.context.getColor(android.R.color.transparent)
                )
                // Для обычных элементов используем цвета темы
                dayText.setTextColor(textColorPrimary)
                dateText.setTextColor(textColorSecondary)
                setsText.setTextColor(textColorPrimary)
                currentDayIcon.visibility = View.INVISIBLE
            }
            
            itemView.setOnClickListener { onClick() }
        }
    }
}
