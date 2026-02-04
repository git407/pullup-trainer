package com.pulluptrainer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatisticsActivity : AppCompatActivity() {

    private lateinit var progressManager: ProgressManager

    private lateinit var recordValueText: TextView
    private lateinit var totalPullupsText: TextView
    private lateinit var completedWorkoutsText: TextView
    private lateinit var completedSetsText: TextView
    private lateinit var dailyRecyclerView: RecyclerView
    private lateinit var dailyChartView: LineChart
    private lateinit var dailyEmptyText: TextView

    private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru"))

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
        setContentView(R.layout.activity_statistics)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.menu_statistics)

        // Устанавливаем белый цвет для кнопки назад
        toolbar.navigationIcon?.let { icon ->
            val wrapped = DrawableCompat.wrap(icon)
            DrawableCompat.setTint(
                wrapped,
                ContextCompat.getColor(this, R.color.white)
            )
            toolbar.navigationIcon = wrapped
        }

        // Устанавливаем белый цвет для иконок меню (три точки)
        toolbar.overflowIcon?.let { icon ->
            val wrapped = DrawableCompat.wrap(icon)
            DrawableCompat.setTint(
                wrapped,
                ContextCompat.getColor(this, R.color.white)
            )
            toolbar.overflowIcon = wrapped
        }

        progressManager = ProgressManager(this)

        recordValueText = findViewById(R.id.recordValueText)
        totalPullupsText = findViewById(R.id.totalPullupsText)
        completedWorkoutsText = findViewById(R.id.completedWorkoutsText)
        completedSetsText = findViewById(R.id.completedSetsText)
        dailyRecyclerView = findViewById(R.id.dailyRecyclerView)
        dailyChartView = findViewById(R.id.dailyChartView)
        dailyEmptyText = findViewById(R.id.dailyEmptyText)

        updateSummary()
        setupDailyList()
    }

    private fun updateSummary() {
        val record = progressManager.getPersonalRecord()
        val totalPullups = progressManager.getTotalPullups()
        val completedWorkouts = progressManager.getCompletedWorkoutsCount()
        val completedSets = progressManager.getCompletedSetsCount()

        recordValueText.text = record.toString()
        totalPullupsText.text = totalPullups.toString()
        completedWorkoutsText.text = completedWorkouts.toString()
        completedSetsText.text = completedSets.toString()
    }

    private fun setupDailyList() {
        val daily = progressManager.getDailyPullups()
        if (daily.isEmpty()) {
            dailyChartView.visibility = View.GONE
            dailyRecyclerView.visibility = View.GONE
            dailyEmptyText.visibility = View.VISIBLE
            return
        }

        dailyChartView.visibility = View.VISIBLE
        dailyRecyclerView.visibility = View.VISIBLE
        dailyEmptyText.visibility = View.GONE

        setupChart(daily)

        val maxValue = daily.maxOfOrNull { it.second } ?: 1
        dailyRecyclerView.layoutManager = LinearLayoutManager(this)
        dailyRecyclerView.adapter = DailyStatsAdapter(daily.reversed(), maxValue)
    }

    private fun setupChart(daily: List<Pair<Long, Int>>) {
        val entries = daily.mapIndexed { index, pair ->
            Entry(index.toFloat(), pair.second.toFloat())
        }

        val dataSet = LineDataSet(entries, getString(R.string.statistics_daily_title)).apply {
            color = ContextCompat.getColor(this@StatisticsActivity, R.color.purple_500)
            setCircleColor(ContextCompat.getColor(this@StatisticsActivity, R.color.purple_500))
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(true)
            valueTextSize = 9f
            valueTextColor = ContextCompat.getColor(this@StatisticsActivity, android.R.color.black)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        // Подписи точек только целыми числами
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry?): String {
                if (entry == null) return ""
                return entry.y.toInt().toString()
            }
        }

        val lineData = LineData(dataSet)
        dailyChartView.data = lineData

        // Ось X – даты
        val xAxis = dailyChartView.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt().coerceIn(0, daily.lastIndex)
                return dateFormat.format(Date(daily[index].first))
            }
        }
        xAxis.labelRotationAngle = -45f
        xAxis.textSize = 8f

        // Левая ось – количество подтягиваний
        val leftAxis = dailyChartView.axisLeft
        leftAxis.textSize = 8f
        leftAxis.granularity = 1f
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }
        dailyChartView.axisRight.isEnabled = false

        // Скролл/зум по горизонтали
        dailyChartView.setTouchEnabled(true)
        dailyChartView.isDragEnabled = true
        dailyChartView.setScaleEnabled(true)
        dailyChartView.setPinchZoom(true)
        dailyChartView.setVisibleXRangeMaximum(5f) // показываем по 5 дней, остальное скроллится

        val desc = Description()
        desc.text = ""
        dailyChartView.description = desc

        dailyChartView.legend.isEnabled = false
        dailyChartView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        updateSummary()
        setupDailyList()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private inner class DailyStatsAdapter(
        private val items: List<Pair<Long, Int>>,
        private val maxValue: Int
    ) : RecyclerView.Adapter<DailyStatsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val dateText: TextView = view.findViewById(R.id.dateText)
            val countText: TextView = view.findViewById(R.id.countText)
            val bar: ProgressBar = view.findViewById(R.id.progressBar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_daily_stat, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (timestamp, count) = items[position]
            holder.dateText.text = dateFormat.format(Date(timestamp))
            holder.countText.text = count.toString()
            holder.bar.max = maxValue
            holder.bar.progress = count
        }

        override fun getItemCount(): Int = items.size
    }
}

