package com.example.cpen321project

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.cpen321project.databinding.ActivityAnalyticsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding
    private lateinit var selected_date: String
    private lateinit var userID: String
    private var emotionsData: Map<String, List<Float>> = emptyMap()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        selected_date = LocalDate.now().toString()
        userID = intent.getStringExtra("USER_ID").toString()
        get_trend()
        setupChart()
        binding.refreshButton.setOnClickListener {
            updateChartData()
        }

        binding.buttonToCalendar.setOnClickListener {
            finish()  // Closes this activity and returns to the previous one
        }
    }

    private fun get_trend() {
        val url = "https://cpen321project-journal.duckdns.org/api/analytics?date=$selected_date&userID=$userID"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("Analytics Fetch", "Failed to fetch analytics data: ${response.code}")
                    return
                }

                val responseBody = response.body?.string() ?: ""
                if (responseBody.isEmpty()) {
                    Log.d("Analytics Fetch", "No analytics data available for this date")
                    return
                }

                try {
                    val jsonResponse = JSONObject(responseBody)

                    val emotionStats = jsonResponse.getJSONObject("emotionStats")
                    val activityStats = jsonResponse.getJSONObject("activityStats")
                    val overallScore = jsonResponse.optDouble("overallScore", -1.0)
                    val summaryArray = jsonResponse.getJSONArray("summary")

                    val summaryList = mutableListOf<String>()
                    for (i in 0 until summaryArray.length()) {
                        val summaryItem = summaryArray.getJSONObject(i)
                        val activity = summaryItem.getString("activity")
                        val emotion = summaryItem.getString("emotion")
                        val displayText = summaryItem.getString("display")
                        summaryList.add("$activity - $emotion: $displayText")
                    }

                    // Convert emotionStats JSON into a usable format
                    val newEmotionsData = mutableMapOf<String, List<Float>>()
                    val keys = emotionStats.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val valuesArray = emotionStats.getJSONArray(key)
                        val valuesList = mutableListOf<Float>()
                        for (i in 0 until valuesArray.length()) {
                            val value = valuesArray.optDouble(i, 0.0)
                            valuesList.add(value.toFloat())
                        }
                        newEmotionsData[key] = valuesList
                    }

                    // Update the class-level emotionsData
                    emotionsData = newEmotionsData

                    runOnUiThread {
                        updateChartData()
                        updateOverallScore(overallScore)
                        updateSummary(summaryList)
                    }
                } catch (e: JSONException) {
                    Log.e("Analytics Fetch", "Error parsing JSON response", e)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("Analytics Fetch", "Error fetching analytics data", e)
            }
        })
    }

    private fun setupChart() {
        val chart = binding.analyticsChart

        // Configure X-Axis
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.labelRotationAngle = 45f
        xAxis.setLabelCount(5, true) // Ensure X-axis has 5 labels

        // Configure Y-Axis
        chart.axisRight.isEnabled = false // Hide right Y-Axis
        chart.axisLeft.setDrawGridLines(false) // Hide grid lines for clarity
        chart.description.isEnabled = false // Remove default description text

        // Enable touch interactions
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)

        // Configure Legend
        val legend = chart.legend
        legend.isEnabled = true
        legend.isWordWrapEnabled = true // Wrap the legend if it's too long
        legend.xEntrySpace = 10f // Adds space between legend items
        legend.yEntrySpace = 5f // Adds vertical spacing
        legend.formSize = 12f // Adjusts the size of legend symbols
    }


    private fun updateChartData() {
        val chart = binding.analyticsChart

        if (emotionsData.isEmpty()) {
            chart.clear()
            chart.setNoDataText("No chart data available")
            return
        }

        val dataSets = mutableListOf<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>()
        val colors = listOf(Color.BLUE, Color.RED, Color.GREEN, Color.MAGENTA, Color.CYAN,
            Color.YELLOW, Color.DKGRAY, Color.LTGRAY, Color.BLACK, Color.WHITE)

        var index = 0
        for ((emotion, values) in emotionsData) {
            if (values.isEmpty()) continue

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

    private fun updateOverallScore(score: Double) {
        runOnUiThread {
            binding.overallScoreText.text = "Overall Score: ${score.toInt()}"
        }
    }

    private fun updateSummary(summaryList: List<String>) {
        runOnUiThread {
            binding.summaryTextView.text = summaryList.joinToString("\n")
        }
    }


}
