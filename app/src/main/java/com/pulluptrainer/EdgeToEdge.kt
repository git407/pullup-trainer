package com.pulluptrainer

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

object EdgeToEdge {
    fun applyToolbarAndBottomInsets(toolbar: View, content: View) {
        applyToolbarInsets(toolbar)
        applyBottomInsets(content)
    }

    fun applyToolbarInsets(toolbar: View) {
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(top = bars.top)
            windowInsets
        }
    }

    fun applyBottomInsets(content: View) {
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = bars.bottom)
            windowInsets
        }
    }
}
