package com.pulluptrainer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class DailyPullupsChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * resources.displayMetrics.density
        color = ContextCompat.getColor(context, R.color.primary)
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.primary)
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * resources.displayMetrics.density
        color = ContextCompat.getColor(context, R.color.light_gray)
    }

    private var entries: List<Pair<Long, Int>> = emptyList()
    private var maxValue: Int = 1

    fun setData(data: List<Pair<Long, Int>>) {
        entries = data
        maxValue = data.maxOfOrNull { it.second } ?: 1
        if (maxValue <= 0) maxValue = 1
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0f || height <= 0f) return

        val paddingH = 16f * resources.displayMetrics.density
        val paddingV = 16f * resources.displayMetrics.density

        val chartLeft = paddingH
        val chartRight = width - paddingH
        val chartTop = paddingV
        val chartBottom = height - paddingV

        if (chartRight <= chartLeft || chartBottom <= chartTop) return

        // Лёгкая горизонтальная линия-сетка по максимуму
        canvas.drawLine(
            chartLeft,
            chartTop,
            chartRight,
            chartTop,
            gridPaint
        )

        val count = entries.size
        val stepX = if (count > 1) (chartRight - chartLeft) / (count - 1).coerceAtLeast(1) else 0f

        val path = Path()
        entries.forEachIndexed { index, (_, value) ->
            val x = chartLeft + stepX * index
            val ratio = value.toFloat() / maxValue.toFloat()
            val y = chartBottom - (chartBottom - chartTop) * ratio

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // Линия
        canvas.drawPath(path, linePaint)

        // Точки
        val radius = 4f * resources.displayMetrics.density
        entries.forEachIndexed { index, (_, value) ->
            val x = chartLeft + stepX * index
            val ratio = value.toFloat() / maxValue.toFloat()
            val y = chartBottom - (chartBottom - chartTop) * ratio
            canvas.drawCircle(x, y, radius, pointPaint)
        }
    }
}

