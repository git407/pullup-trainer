package com.pulluptrainer

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.io.IOException
import java.util.Random

class SettingsActivity : AppCompatActivity() {
    private lateinit var soundSwitch: Switch
    private lateinit var notificationsSwitch: Switch
    private lateinit var assistantSpinner: Spinner
    private lateinit var themeSpinner: Spinner
    private lateinit var testSoundButton: Button
    private lateinit var settingsManager: SettingsManager
    private var testMediaPlayer: MediaPlayer? = null
    private var isPlaying: Boolean = false
    private var themeChanged: Boolean = false
    
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
        setContentView(R.layout.activity_settings)
        
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        settingsManager = SettingsManager(this)
        
        soundSwitch = findViewById(R.id.soundSwitch)
        notificationsSwitch = findViewById(R.id.notificationsSwitch)
        assistantSpinner = findViewById(R.id.assistantSpinner)
        themeSpinner = findViewById(R.id.themeSpinner)
        testSoundButton = findViewById(R.id.testSoundButton)
        
        // Загружаем текущие настройки
        soundSwitch.isChecked = settingsManager.isSoundEnabled()
        notificationsSwitch.isChecked = settingsManager.areNotificationsEnabled()
        
        // Настраиваем выбор темы
        val themes = listOf(
            getString(R.string.settings_theme_system),
            getString(R.string.settings_theme_light),
            getString(R.string.settings_theme_dark)
        )
        val themeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            themes
        )
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        themeSpinner.adapter = themeAdapter
        
        // Устанавливаем выбранную тему
        val savedTheme = settingsManager.getTheme()
        val themeIndex = when (savedTheme) {
            SettingsManager.THEME_LIGHT -> 1
            SettingsManager.THEME_DARK -> 2
            else -> 0 // Системная
        }
        themeSpinner.setSelection(themeIndex)
        
        // Сохраняем тему при изменении
        var isInitialSelection = true
        themeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                // Пропускаем первый вызов (инициализация)
                if (isInitialSelection) {
                    isInitialSelection = false
                    return
                }
                
                val theme = when (position) {
                    1 -> SettingsManager.THEME_LIGHT
                    2 -> SettingsManager.THEME_DARK
                    else -> SettingsManager.THEME_SYSTEM
                }
                val currentTheme = settingsManager.getTheme()
                if (theme != currentTheme) {
                    settingsManager.setTheme(theme)
                    themeChanged = true
                    // Устанавливаем результат для обновления темы в MainActivity
                    setResult(RESULT_OK)
                    // Применяем тему сразу при выборе
                    // Используем post для отложенного выполнения, чтобы не блокировать UI
                    themeSpinner.post {
                        recreate()
                    }
                }
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        // Загружаем список ассистентов
        val assistants = getAvailableAssistants()
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            assistants
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        assistantSpinner.adapter = adapter
        
        // Устанавливаем выбранного ассистента
        val savedAssistant = settingsManager.getSelectedAssistant()
        val selectedIndex = if (savedAssistant != null && assistants.contains(savedAssistant)) {
            assistants.indexOf(savedAssistant)
        } else {
            0 // "Нет"
        }
        assistantSpinner.setSelection(selectedIndex)
        
        // Сохраняем настройки при изменении
        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setSoundEnabled(isChecked)
        }
        
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setNotificationsEnabled(isChecked)
            // Если уведомления отключены, отменяем запланированные уведомления
            if (!isChecked) {
                val notificationHelper = NotificationHelper(this)
                notificationHelper.cancelNotification()
            }
        }
        
        assistantSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selected = if (position == 0) null else assistants[position]
                settingsManager.setSelectedAssistant(selected)
                // Обновляем состояние кнопки теста
                updateTestButtonState(selected)
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        // Кнопка тестирования звука (переключатель плей/стоп)
        testSoundButton.setOnClickListener {
            if (isPlaying) {
                stopTestSound()
            } else {
                val currentAssistant = if (assistantSpinner.selectedItemPosition == 0) null else assistants[assistantSpinner.selectedItemPosition]
                playTestSound(currentAssistant)
            }
        }
        
        // Обновляем состояние кнопки при загрузке
        updateTestButtonState(savedAssistant)
    }
    
    private fun updateTestButtonState(assistant: String?) {
        testSoundButton.isEnabled = assistant != null
    }
    
    private fun playTestSound(assistant: String?) {
        if (assistant == null) {
            return
        }
        
        try {
            // Останавливаем предыдущее воспроизведение, если есть
            testMediaPlayer?.release()
            
            // Получаем список звуковых файлов ассистента
            val soundFiles = getAssistantSoundFiles(assistant)
            if (soundFiles.isEmpty()) {
                return
            }
            
            // Выбираем случайный файл
            val random = Random()
            val randomFile = soundFiles[random.nextInt(soundFiles.size)]
            
            // Проигрываем звук
            val assetFileDescriptor: AssetFileDescriptor = assets.openFd("sound/$assistant/$randomFile")
            testMediaPlayer = MediaPlayer().apply {
                setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                prepare()
                start()
            }
            isPlaying = true
            updateTestButtonIcon()
            testMediaPlayer?.setOnCompletionListener {
                testMediaPlayer?.release()
                testMediaPlayer = null
                isPlaying = false
                updateTestButtonIcon()
            }
            assetFileDescriptor.close()
        } catch (e: IOException) {
            e.printStackTrace()
            isPlaying = false
            updateTestButtonIcon()
        } catch (e: Exception) {
            e.printStackTrace()
            isPlaying = false
            updateTestButtonIcon()
        }
    }
    
    private fun stopTestSound() {
        testMediaPlayer?.release()
        testMediaPlayer = null
        isPlaying = false
        updateTestButtonIcon()
    }
    
    private fun updateTestButtonIcon() {
        testSoundButton.text = if (isPlaying) "❚❚" else "▶"
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
    
    private fun getAvailableAssistants(): List<String> {
        val assistants = mutableListOf<String>()
        assistants.add(getString(R.string.settings_no_assistant)) // "Нет" - первый элемент
        
        try {
            val assetManager: AssetManager = assets
            val soundDir = "sound"
            
            // Проверяем, существует ли директория sound
            val soundDirFiles = assetManager.list(soundDir)
            if (soundDirFiles == null) {
                // Директория не существует
                return assistants
            }
            
            soundDirFiles.forEach { file ->
                try {
                    // Проверяем, что это директория (ассистент)
                    val subFiles = assetManager.list("$soundDir/$file")
                    if (subFiles != null && subFiles.isNotEmpty()) {
                        // Проверяем, что в директории есть мотивационные файлы (motivate*)
                        val hasMotivationalFiles = subFiles.any { soundFile ->
                            val lowerFile = soundFile.lowercase()
                            (lowerFile.endsWith(".mp3", ignoreCase = true) ||
                            lowerFile.endsWith(".wav", ignoreCase = true) ||
                            lowerFile.endsWith(".ogg", ignoreCase = true) ||
                            lowerFile.endsWith(".m4a", ignoreCase = true)) &&
                            lowerFile.startsWith("motivate")
                        }
                        if (hasMotivationalFiles) {
                            // Это директория с мотивационными файлами - добавляем как ассистента
                            assistants.add(file)
                        }
                    }
                } catch (e: Exception) {
                    // Игнорируем ошибки при проверке отдельных файлов
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return assistants
    }
    
    override fun onPause() {
        super.onPause()
        // Останавливаем воспроизведение при паузе
        stopTestSound()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Освобождаем ресурсы
        stopTestSound()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onBackPressed() {
        // Просто закрываем активность, тема уже применена при выборе
        super.onBackPressed()
    }
}
