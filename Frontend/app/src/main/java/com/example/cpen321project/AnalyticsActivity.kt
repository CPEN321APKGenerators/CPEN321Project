package com.example.cpen321project

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.cpen321project.databinding.ActivityAnalyticsBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupChart()
        binding.refreshButton.setOnClickListener {
            updateChartData()
        }
    }

    private fun setupChart() {
        val chart = binding.analyticsChart

        // Configure X-Axis
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.labelRotationAngle = 45f
        // Configure Y-Axis
        chart.axisRight.isEnabled = false // Hide right Y-Axis
        updateChartData()
    }

    private fun updateChartData() {
        val chart = binding.analyticsChart

        val emotionsData = mapOf(
            "Joy" to listOf(10f, 20f, 30f, 25f, 35f),
            "Stress" to listOf(50f, 40f, 30f, 20f, 10f),
            "Sadness" to listOf(5f, 10f, 15f, 12f, 20f)
        )

        if (emotionsData.isEmpty()) {
            chart.clear()
            chart.setNoDataText("No chart data available")
            return
        }

        val dataSets = mutableListOf<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>()
        val colors = listOf(Color.BLUE, Color.RED, Color.GREEN)

        var index = 0
        for ((emotion, values) in emotionsData) {
            if (values.isEmpty()) continue // 🔥 Ensure we don't add empty datasets

            val entries = values.mapIndexed { i, value -> Entry(i.toFloat(), value) }
            val dataSet = LineDataSet(entries, emotion)
            dataSet.color = colors[index % colors.size]
            dataSet.valueTextColor = Color.BLACK
            dataSets.add(dataSet)
            index++
        }

        if (dataSets.isEmpty()) {
            chart.clear()
            chart.setNoDataText("No valid data to display")
            return
        }

        chart.data = LineData(dataSets)
        chart.invalidate()
    }


}
