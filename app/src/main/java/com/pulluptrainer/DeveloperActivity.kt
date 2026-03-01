package com.pulluptrainer

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

class DeveloperActivity : AppCompatActivity() {

    private lateinit var progressManager: ProgressManager

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
        applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.developer_title)

        // Белая кнопка назад
        toolbar.navigationIcon?.let { icon ->
            val wrapped = DrawableCompat.wrap(icon)
            DrawableCompat.setTint(
                wrapped,
                ContextCompat.getColor(this, R.color.white)
            )
            toolbar.navigationIcon = wrapped
        }

        toolbar.setNavigationOnClickListener { finish() }

        progressManager = ProgressManager(this)

        val fillTestDataButton: Button = findViewById(R.id.fillTestDataButton)
        fillTestDataButton.setOnClickListener {
            progressManager.fillTestDataForDebug()
            Toast.makeText(this, getString(R.string.developer_test_data_filled), Toast.LENGTH_LONG).show()
        }
        findViewById<Button>(R.id.testNotificationButton).setOnClickListener {
            NotificationHelper(this).showWorkoutReminder(this, progressManager.getCurrentLevel(), progressManager.getCurrentDay())
            Toast.makeText(this, getString(R.string.developer_notification_shown), Toast.LENGTH_SHORT).show()
        }
    }
}

