package com.pulluptrainer

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

class AboutActivity : AppCompatActivity() {
    private var logoTapCount = 0
    private val tapsToActivate = 7
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
        setContentView(R.layout.activity_about)
        
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // Устанавливаем белый цвет для кнопки назад
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
        
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Пасхалка: 7 нажатий по логотипу открывают экран разработчика
        val logoView: ImageView = findViewById(R.id.logoImageView)
        logoView.setOnClickListener {
            logoTapCount++
            if (logoTapCount >= tapsToActivate) {
                logoTapCount = 0
                startActivity(Intent(this, DeveloperActivity::class.java))
            } else if (logoTapCount >= tapsToActivate - 3) {
                // Лёгкий подсказочный тост под конец серии нажатий
                val remaining = tapsToActivate - logoTapCount
                Toast.makeText(
                    this,
                    "До экрана разработчика осталось нажатий: $remaining",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
