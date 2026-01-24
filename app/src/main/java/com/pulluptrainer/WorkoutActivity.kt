package com.pulluptrainer

import android.content.res.AssetFileDescriptor
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import android.graphics.Typeface
import java.io.IOException
import java.util.Random

class WorkoutActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var progressBar: LinearLayout
    private lateinit var repsText: TextView
    private lateinit var exerciseText: TextView
    private lateinit var statusText: TextView
    private lateinit var overallCountText: TextView
    private lateinit var recordText: TextView
    private lateinit var timerText: TextView
    private lateinit var startButton: TextView
    private lateinit var completeButton: TextView
    private lateinit var cancelButton: TextView
    private lateinit var skipButton: TextView
    
    private var level: Int = 1
    private var day: Int = 1
    private var sets: List<Int> = emptyList()
    private var currentSetIndex: Int = -1
    private var currentRepIndex: Int = 0
    private var countDownTimer: CountDownTimer? = null
    private var totalRepsRemaining: Int = 0
    
    // Датчики для автоматического подсчета
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var isSensorActive: Boolean = false
    
    // Звук для подсчета подтягиваний
    private var toneGenerator: ToneGenerator? = null
    
    // Настройки
    private lateinit var settingsManager: SettingsManager
    
    // Медиаплеер для звуков ассистента
    private var assistantMediaPlayer: MediaPlayer? = null
    private var isAssistantSoundPlaying: Boolean = false
    
    // Медиаплеер для звука ready.mp3
    private var readyMediaPlayer: MediaPlayer? = null
    private var isReadySoundPlaying: Boolean = false
    
    // Медиаплеер для звука record.mp3
    private var recordMediaPlayer: MediaPlayer? = null
    private var isRecordSoundPlaying: Boolean = false
    
    // Медиаплеер для звука pullup.mp3
    private var pullupMediaPlayer: MediaPlayer? = null
    private var isPullupSoundPlaying: Boolean = false
    
    // Личный рекорд
    private var personalRecord: Int = 0
    private var initialRecord: Int = 0 // Начальный рекорд до начала тренировки (для проверки, был ли рекорд установлен)
    private var totalRepsInWorkout: Int = 0 // Общее количество подтягиваний в текущей тренировке
    private var recordSoundPlayed: Boolean = false // Флаг, был ли уже проигран звук record.mp3 в текущей тренировке
    
    // Переменные для определения подтягивания
    private var lastY: Float = 0f
    private var lastVelocity: Float = 0f
    private var isMovingUp: Boolean = false
    private var peakUp: Float = 0f
    private var peakDown: Float = 0f
    private var lastRepTime: Long = 0
    private var gravityY: Float = 0f // Гравитация по оси Y для фильтрации
    private var samplesCount: Int = 0
    private val MIN_AMPLITUDE = 4.0f // Минимальная амплитуда для определения подтягивания (м/с²)
    private val MIN_TIME_BETWEEN_REPS = 1000L // Минимальное время между подтягиваниями (мс)
    private val GRAVITY_SAMPLES = 50 // Количество сэмплов для калибровки гравитации
    
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
        setContentView(R.layout.activity_workout)
        
        level = intent.getIntExtra("level", 1)
        day = intent.getIntExtra("day", 1)
        
        val workoutDay = WorkoutData.levels[level - 1].days[day - 1]
        sets = workoutDay.sets
        
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Уровень $level - День ${String.format("%02d", day)}"
        
        // Устанавливаем белый цвет для кнопки назад
        supportActionBar?.setHomeAsUpIndicator(null)
        toolbar.navigationIcon?.let { icon ->
            val wrapped = DrawableCompat.wrap(icon)
            DrawableCompat.setTint(wrapped, ContextCompat.getColor(this, R.color.white))
            toolbar.navigationIcon = wrapped
        }
        
        // Устанавливаем белый цвет для иконок меню (три точки)
        toolbar.overflowIcon?.let { icon ->
            val wrapped = DrawableCompat.wrap(icon)
            DrawableCompat.setTint(wrapped, ContextCompat.getColor(this, R.color.white))
            toolbar.overflowIcon = wrapped
        }
        
        progressBar = findViewById(R.id.progressBar)
        repsText = findViewById(R.id.repsText)
        exerciseText = findViewById(R.id.exerciseText)
        statusText = findViewById(R.id.statusText)
        overallCountText = findViewById(R.id.overallCountText)
        recordText = findViewById(R.id.recordText)
        timerText = findViewById(R.id.timerText)
        startButton = findViewById(R.id.startButton)
        completeButton = findViewById(R.id.completeButton)
        cancelButton = findViewById(R.id.cancelButton)
        skipButton = findViewById(R.id.skipButton)
        
        // Вычисляем общее количество повторений
        totalRepsRemaining = sets.sum()
        overallCountText.text = totalRepsRemaining.toString()
        
        updateProgressBar()
        updateDisplay()
        
        startButton.setOnClickListener {
            startWorkout()
        }
        
        completeButton.setOnClickListener {
            completeSet()
        }
        
        cancelButton.setOnClickListener {
            cancelWorkout()
        }
        
        skipButton.setOnClickListener {
            skipRest()
        }
        
        // Инициализация датчиков
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        // Инициализация генератора звука
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Инициализация настроек
        settingsManager = SettingsManager(this)
        
        // Загружаем личный рекорд
        val progressManager = ProgressManager(this)
        personalRecord = progressManager.getPersonalRecord()
        initialRecord = personalRecord // Сохраняем начальный рекорд до начала тренировки
        recordText.text = personalRecord.toString()
        
        // Сбрасываем счетчик подтягиваний в тренировке и флаг звука рекорда
        totalRepsInWorkout = 0
        recordSoundPlayed = false
    }
    
    private fun updateProgressBar() {
        progressBar.removeAllViews()
        
        for (i in sets.indices) {
            val isActive = i == currentSetIndex && currentSetIndex >= 0
            
            // Создаем TextView для номера подхода
            val setView = TextView(this).apply {
                text = sets[i].toString()
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                setPadding(
                    (16 * resources.displayMetrics.density).toInt(),
                    (8 * resources.displayMetrics.density).toInt(),
                    (16 * resources.displayMetrics.density).toInt(),
                    (8 * resources.displayMetrics.density).toInt()
                )
                
                if (isActive) {
                    // Активный подход - темный фон, белый текст
                    setBackgroundColor(ContextCompat.getColor(this@WorkoutActivity, R.color.dark_gray))
                    setTextColor(ContextCompat.getColor(this@WorkoutActivity, android.R.color.white))
                } else {
                    // Неактивный подход - прозрачный фон, темный текст
                    setTextColor(ContextCompat.getColor(this@WorkoutActivity, R.color.dark_gray))
                }
            }
            
            progressBar.addView(setView)
        }
    }
    
    private fun updateDisplay() {
        if (currentSetIndex < 0) {
            // Тренировка еще не начата
            repsText.text = sets.sum().toString()
            statusText.text = "ОСТАЛОСЬ"
            return
        }
        
        if (currentSetIndex >= sets.size) {
            // Все подходы завершены
            repsText.text = "0"
            statusText.text = "ЗАВЕРШЕНО"
            return
        }
        
        val currentSetReps = sets[currentSetIndex]
        val remainingInSet = maxOf(0, currentSetReps - currentRepIndex) // Гарантируем, что значение не будет отрицательным
        
        repsText.text = remainingInSet.toString()
        statusText.text = "ОСТАЛОСЬ"
        
        // Обновляем общий прогресс
        var totalRemaining = 0
        for (i in currentSetIndex until sets.size) {
            if (i == currentSetIndex) {
                totalRemaining += remainingInSet
            } else {
                totalRemaining += sets[i]
            }
        }
        totalRepsRemaining = totalRemaining
        overallCountText.text = totalRepsRemaining.toString()
    }
    
    private fun startWorkout() {
        currentSetIndex = 0
        currentRepIndex = 0
        totalRepsInWorkout = 0 // Сбрасываем счетчик при начале тренировки
        recordSoundPlayed = false // Сбрасываем флаг звука рекорда при начале тренировки
        initialRecord = personalRecord // Сохраняем начальный рекорд до начала тренировки
        updateProgressBar()
        updateDisplay()
        startButton.visibility = View.GONE
        completeButton.visibility = View.VISIBLE
        cancelButton.visibility = View.VISIBLE
        
        // Воспроизводим ready.mp3 перед началом первого подхода
        if (settingsManager.isSoundEnabled()) {
            playReadySound {
                // Запускаем датчики для автоматического подсчета после завершения звука
                startSensorTracking()
            }
        } else {
            // Запускаем датчики для автоматического подсчета
            startSensorTracking()
        }
    }
    
    private fun startSensorTracking() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            isSensorActive = true
            lastY = 0f
            lastVelocity = 0f
            isMovingUp = false
            peakUp = 0f
            peakDown = 0f
            lastRepTime = 0
            gravityY = 0f
            samplesCount = 0
        }
    }
    
    private fun stopSensorTracking() {
        if (isSensorActive) {
            sensorManager?.unregisterListener(this)
            isSensorActive = false
        }
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || currentSetIndex < 0 || currentSetIndex >= sets.size) return
        
        // Не учитываем подтягивания, пока играет ready.mp3
        if (isReadySoundPlaying) return
        
        // Используем ось Y для определения вертикального движения
        val y = event.values[1] // Ось Y (вертикальная)
        val currentTime = System.currentTimeMillis()
        
        // Калибруем гравитацию в первые несколько сэмплов
        if (samplesCount < GRAVITY_SAMPLES) {
            gravityY = (gravityY * samplesCount + y) / (samplesCount + 1)
            samplesCount++
            return
        }
        
        // Фильтруем гравитацию - получаем только ускорение движения
        val acceleration = y - gravityY
        
        // Определяем скорость изменения ускорения
        val velocity = acceleration - lastY
        lastY = acceleration
        
        // Определяем направление движения
        if (velocity > 1.0f && !isMovingUp) {
            // Начало движения вверх (подтягивание)
            isMovingUp = true
            peakUp = acceleration
        } else if (isMovingUp) {
            // Отслеживаем пик движения вверх
            if (acceleration > peakUp) {
                peakUp = acceleration
            }
            
            // Если началось движение вниз (опускание)
            if (velocity < -1.0f) {
                peakDown = acceleration
                val amplitude = peakUp - peakDown
                
                // Проверяем, достаточно ли амплитуды для подтягивания
                if (amplitude > MIN_AMPLITUDE && 
                    (currentTime - lastRepTime) > MIN_TIME_BETWEEN_REPS) {
                    // Засчитываем подтягивание
                    lastRepTime = currentTime
                    runOnUiThread {
                        completeSet()
                    }
                }
                isMovingUp = false
                peakUp = 0f
                peakDown = 0f
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Не требуется
    }
    
    private fun completeSet() {
        if (currentSetIndex < 0) return
        
        // Увеличиваем счетчик повторений в текущем подходе
        currentRepIndex++
        
        // Увеличиваем общее количество подтягиваний в тренировке
        totalRepsInWorkout++
        
        // Увеличиваем общее количество подтягиваний в базе данных
        val progressManager = ProgressManager(this)
        progressManager.addTotalPullups(1)
        
        // Проверяем, является ли это последним подтягиванием в подходе
        val isLastRepInSet = currentRepIndex >= sets[currentSetIndex]
        
        // Сохраняем старое значение рекорда перед обновлением
        val oldRecord = personalRecord
        
        // Проверяем, устанавливается ли новый рекорд
        val isNewRecord = totalRepsInWorkout > personalRecord
        
        // Обновляем личный рекорд, если текущее количество превышает рекорд
        if (isNewRecord) {
            personalRecord = totalRepsInWorkout
            progressManager.setPersonalRecord(personalRecord)
            recordText.text = personalRecord.toString()
        }
        
        // Воспроизводим звук при засчитывании подтягивания (если включен)
        if (settingsManager.isSoundEnabled()) {
            if (isNewRecord && !recordSoundPlayed && initialRecord > 0) {
                // Если установлен рекорд и начальный рекорд был больше 0 (рекорд был установлен ДО начала тренировки),
                // звук еще не проигрывался в этой тренировке, проигрываем record.mp3
                // Не воспроизводим звук, если начальный рекорд был 0 (рекорда не было до начала тренировки)
                recordSoundPlayed = true
                playRecordSound()
            } else if (!isLastRepInSet) {
                // Иначе проигрываем pullup.mp3, но только если это не последнее подтягивание в подходе
                // (при последнем подтягивании будет проигрываться done.mp3)
                playPullupSound()
            }
        }
        
        // Проверяем, завершен ли текущий подход
        if (isLastRepInSet) {
            // Подход завершен, увеличиваем счетчик завершенных подходов
            progressManager.addCompletedSet()
            
            // Если это последний подход, обновляем отображение чтобы показать 0 перед завершением
            if (currentSetIndex >= sets.size - 1) {
                // Обновляем отображение чтобы показать 0
                repsText.text = "0"
                statusText.text = "ЗАВЕРШЕНО"
                overallCountText.text = "0"
                
                // Отмечаем тренировку как выполненную
                progressManager.markWorkoutCompleted(level, day)
                // Сбрасываем активную тренировку
                progressManager.clearActiveWorkout()
                
                // Показываем сообщение о завершении тренировки
                Toast.makeText(this, getString(R.string.workout_completed), Toast.LENGTH_LONG).show()
                
                // Проигрываем звук ассистента при завершении тренировки
                // Закрываем активность только после завершения звука
                playAssistantSound {
                    finish()
                }
                return
            }
            
            // Обновляем отображение перед переходом к следующему подходу (чтобы счетчик показал 0)
            updateDisplay()
            
            // Подход завершен, переходим к следующему
            currentRepIndex = 0
            
            // Воспроизводим звук done.mp3 при завершении подхода (если включен звук и выбран ассистент)
            if (settingsManager.isSoundEnabled()) {
                playDoneSound()
            }
            
            // Запускаем таймер отдыха перед следующим подходом
            startRestTimer()
        } else {
            // Подход еще не завершен, обновляем отображение
            updateDisplay()
        }
    }
    
    private fun startRestTimer() {
        // Останавливаем датчики во время отдыха
        stopSensorTracking()
        
        // Скрываем кнопки и показываем таймер
        completeButton.visibility = View.GONE
        cancelButton.visibility = View.VISIBLE // Кнопка "ПРЕРВАТЬ" всегда видна
        repsText.visibility = View.GONE
        exerciseText.visibility = View.GONE
        statusText.visibility = View.GONE
        timerText.visibility = View.VISIBLE
        skipButton.visibility = View.VISIBLE
        timerText.text = "01:00" // Устанавливаем начальное значение
        
        // Отменяем предыдущий таймер, если он есть
        countDownTimer?.cancel()
        
        // Создаем таймер на 60 секунд (1 минута)
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val totalSeconds = (millisUntilFinished / 1000).toInt()
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                timerText.text = String.format("%02d:%02d", minutes, seconds)
            }
            
            override fun onFinish() {
                moveToNextSet()
            }
        }
        
        countDownTimer?.start()
    }
    
    private fun skipRest() {
        // Отменяем таймер и переходим к следующему подходу
        countDownTimer?.cancel()
        moveToNextSet()
    }
    
    private fun moveToNextSet() {
        timerText.text = "00:00"
        timerText.visibility = View.GONE
        skipButton.visibility = View.GONE
        repsText.visibility = View.VISIBLE
        exerciseText.visibility = View.VISIBLE
        statusText.visibility = View.VISIBLE
        
        // Переходим к следующему подходу
        currentSetIndex++
        currentRepIndex = 0
        
        // Обновляем отображение
        updateProgressBar()
        updateDisplay()
        
        // Показываем кнопки снова
        completeButton.visibility = View.VISIBLE
        cancelButton.visibility = View.VISIBLE
        
        // Возобновляем отслеживание датчиков после воспроизведения ready.mp3
        if (currentSetIndex < sets.size) {
            if (settingsManager.isSoundEnabled()) {
                playReadySound {
                    startSensorTracking()
                }
            } else {
                startSensorTracking()
            }
        }
    }
    
    private fun cancelWorkout() {
        countDownTimer?.cancel()
        stopSensorTracking()
        // Сбрасываем активную тренировку при отмене
        val progressManager = ProgressManager(this)
        progressManager.clearActiveWorkout()
        finish()
    }
    
    override fun onPause() {
        super.onPause()
        stopSensorTracking()
        // Останавливаем воспроизведение звуков
        recordMediaPlayer?.release()
        recordMediaPlayer = null
        isRecordSoundPlaying = false
        pullupMediaPlayer?.release()
        pullupMediaPlayer = null
        isPullupSoundPlaying = false
    }
    
    override fun onResume() {
        super.onResume()
        // Возобновляем датчики, если тренировка активна
        if (currentSetIndex >= 0 && currentSetIndex < sets.size && !isSensorActive) {
            startSensorTracking()
        }
    }
    
    private fun playPullupSound() {
        val selectedAssistant = settingsManager.getSelectedAssistantWithDefault()
        if (selectedAssistant == null || !settingsManager.isSoundEnabled()) {
            return
        }
        
        // Если звук уже играет, не запускаем новый
        if (isPullupSoundPlaying || pullupMediaPlayer?.isPlaying == true) {
            return
        }
        
        try {
            // Останавливаем предыдущее воспроизведение, если есть
            pullupMediaPlayer?.release()
            pullupMediaPlayer = null
            
            // Пытаемся воспроизвести pullup.mp3
            val assetFileDescriptor: AssetFileDescriptor = assets.openFd("sound/$selectedAssistant/pullup.mp3")
            isPullupSoundPlaying = true
            pullupMediaPlayer = MediaPlayer().apply {
                setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                prepare()
                start()
                setOnCompletionListener {
                    release()
                    pullupMediaPlayer = null
                    isPullupSoundPlaying = false
                }
            }
            assetFileDescriptor.close()
        } catch (e: IOException) {
            // Файл pullup.mp3 не найден - это нормально, просто не воспроизводим
            isPullupSoundPlaying = false
        } catch (e: Exception) {
            e.printStackTrace()
            isPullupSoundPlaying = false
        }
    }
    
    private fun playAssistantSound(onComplete: () -> Unit = {}) {
        // Если звук уже играет, не запускаем новый
        if (isAssistantSoundPlaying || assistantMediaPlayer?.isPlaying == true) {
            return
        }
        
        val selectedAssistant = settingsManager.getSelectedAssistantWithDefault()
        if (selectedAssistant == null || !settingsManager.isSoundEnabled()) {
            // Если звук не включен или ассистент не выбран, сразу вызываем callback
            onComplete()
            return
        }
        
        try {
            // Останавливаем предыдущее воспроизведение, если есть
            assistantMediaPlayer?.release()
            assistantMediaPlayer = null
            
            // Получаем список звуковых файлов ассистента
            val soundFiles = getAssistantSoundFiles(selectedAssistant)
            if (soundFiles.isEmpty()) {
                // Если файлов нет, сразу вызываем callback
                onComplete()
                return
            }
            
            // Выбираем случайный файл
            val random = Random()
            val randomFile = soundFiles[random.nextInt(soundFiles.size)]
            
            // Проигрываем звук
            val assetFileDescriptor: AssetFileDescriptor = assets.openFd("sound/$selectedAssistant/$randomFile")
            isAssistantSoundPlaying = true
            assistantMediaPlayer = MediaPlayer().apply {
                setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                prepare()
                start()
                setOnCompletionListener {
                    release()
                    assistantMediaPlayer = null
                    isAssistantSoundPlaying = false
                    // Вызываем callback после завершения воспроизведения
                    onComplete()
                }
            }
            assetFileDescriptor.close()
        } catch (e: IOException) {
            e.printStackTrace()
            isAssistantSoundPlaying = false
            // При ошибке тоже вызываем callback
            onComplete()
        } catch (e: Exception) {
            e.printStackTrace()
            isAssistantSoundPlaying = false
            // При ошибке тоже вызываем callback
            onComplete()
        }
    }
    
    private fun playReadySound(onComplete: () -> Unit = {}) {
        val selectedAssistant = settingsManager.getSelectedAssistantWithDefault()
        if (selectedAssistant == null || !settingsManager.isSoundEnabled()) {
            onComplete()
            return
        }
        
        try {
            // Останавливаем предыдущее воспроизведение, если есть
            readyMediaPlayer?.release()
            readyMediaPlayer = null
            
            // Пытаемся воспроизвести ready.mp3
            val assetFileDescriptor: AssetFileDescriptor = assets.openFd("sound/$selectedAssistant/ready.mp3")
            isReadySoundPlaying = true
            readyMediaPlayer = MediaPlayer().apply {
                setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                prepare()
                start()
                setOnCompletionListener {
                    release()
                    readyMediaPlayer = null
                    isReadySoundPlaying = false
                    onComplete()
                }
            }
            assetFileDescriptor.close()
        } catch (e: IOException) {
            // Файл ready.mp3 не найден - это нормально, просто не воспроизводим
            isReadySoundPlaying = false
            onComplete()
        } catch (e: Exception) {
            e.printStackTrace()
            isReadySoundPlaying = false
            onComplete()
        }
    }
    
    private fun playDoneSound() {
        val selectedAssistant = settingsManager.getSelectedAssistantWithDefault()
        if (selectedAssistant == null || !settingsManager.isSoundEnabled()) {
            return
        }
        
        try {
            // Пытаемся воспроизвести done.mp3
            val assetFileDescriptor: AssetFileDescriptor = assets.openFd("sound/$selectedAssistant/done.mp3")
            val doneMediaPlayer = MediaPlayer().apply {
                setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                prepare()
                start()
                setOnCompletionListener {
                    release()
                }
            }
            assetFileDescriptor.close()
        } catch (e: IOException) {
            // Файл done.mp3 не найден - это нормально, просто не воспроизводим
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun playRecordSound() {
        val selectedAssistant = settingsManager.getSelectedAssistantWithDefault()
        if (selectedAssistant == null || !settingsManager.isSoundEnabled()) {
            return
        }
        
        // Если звук уже играет, не запускаем новый
        if (isRecordSoundPlaying || recordMediaPlayer?.isPlaying == true) {
            return
        }
        
        try {
            // Останавливаем предыдущее воспроизведение, если есть
            recordMediaPlayer?.release()
            recordMediaPlayer = null
            
            // Пытаемся воспроизвести record.mp3
            val assetFileDescriptor: AssetFileDescriptor = assets.openFd("sound/$selectedAssistant/record.mp3")
            isRecordSoundPlaying = true
            recordMediaPlayer = MediaPlayer().apply {
                setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                prepare()
                start()
                setOnCompletionListener {
                    release()
                    recordMediaPlayer = null
                    isRecordSoundPlaying = false
                }
            }
            assetFileDescriptor.close()
        } catch (e: IOException) {
            // Файл record.mp3 не найден - это нормально, просто не воспроизводим
            isRecordSoundPlaying = false
        } catch (e: Exception) {
            e.printStackTrace()
            isRecordSoundPlaying = false
        }
    }
    
    private fun getAssistantSoundFiles(assistantName: String): List<String> {
        val soundFiles = mutableListOf<String>()
        try {
            val files = assets.list("sound/$assistantName")
            files?.forEach { file ->
                // Проверяем, что это звуковой файл (case-insensitive)
                val lowerFile = file.lowercase()
                // Фильтруем только мотивационные файлы (motivate*)
                if ((lowerFile.endsWith(".mp3") || lowerFile.endsWith(".wav") || 
                    lowerFile.endsWith(".ogg") || lowerFile.endsWith(".m4a")) &&
                    lowerFile.startsWith("motivate")) {
                    soundFiles.add(file)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return soundFiles
    }
    
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        stopSensorTracking()
        toneGenerator?.release()
        toneGenerator = null
        
        // Освобождаем readyMediaPlayer
        if (readyMediaPlayer != null) {
            try {
                if (readyMediaPlayer?.isPlaying == true) {
                    readyMediaPlayer?.stop()
                }
                readyMediaPlayer?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            readyMediaPlayer = null
            isReadySoundPlaying = false
        }
        
        // Не освобождаем assistantMediaPlayer здесь, если он все еще играет
        // Он освободится сам в setOnCompletionListener
        // Но если активность уничтожается принудительно (например, при повороте экрана),
        // то нужно освободить ресурсы
        if (assistantMediaPlayer != null) {
            try {
                if (assistantMediaPlayer?.isPlaying == true) {
                    assistantMediaPlayer?.stop()
                }
                assistantMediaPlayer?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            assistantMediaPlayer = null
        }
        
        // Освобождаем recordMediaPlayer
        if (recordMediaPlayer != null) {
            try {
                if (recordMediaPlayer?.isPlaying == true) {
                    recordMediaPlayer?.stop()
                }
                recordMediaPlayer?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            recordMediaPlayer = null
            isRecordSoundPlaying = false
        }
        
        // Освобождаем pullupMediaPlayer
        if (pullupMediaPlayer != null) {
            try {
                if (pullupMediaPlayer?.isPlaying == true) {
                    pullupMediaPlayer?.stop()
                }
                pullupMediaPlayer?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            pullupMediaPlayer = null
            isPullupSoundPlaying = false
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onBackPressed() {
        cancelWorkout()
    }
}

