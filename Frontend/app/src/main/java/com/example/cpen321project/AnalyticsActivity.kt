package com.example.cpen321project

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.cpen321project.databinding.ActivityAnalyticsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale
import java.util.Random

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding
    private lateinit var selected_date: String
    private lateinit var userID: String
    private var emotionsData: Map<String, List<Float>> = emptyMap()
    private val selectedEmotions = mutableSetOf<String>()
    private lateinit var filterButton: Button

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
        val url =
            "https://cpen321project-journal.duckdns.org/api/analytics?date=$selected_date&userID=$userID"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("Analytics Fetch", "Failed to fetch analytics data: ${response.code}")
                    useFallbackData() // Use random data instead
                    return
                }

                val responseBody = response.body?.string() ?: ""
                if (responseBody.isEmpty()) {
                    Log.d("Analytics Fetch", "No analytics data available for this date")
                    useFallbackData() // Use random data instead
                    return
                }

                try {
                    val jsonResponse = JSONObject(responseBody)

                    val emotionStats = jsonResponse.getJSONObject("emotionStats")
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

                    emotionsData = newEmotionsData
                    runOnUiThread {
                        updateChartData()
                        updateOverallScore(overallScore)
                        updateSummary(summaryList)
                        setupEmotionFilter()
                    }
                } catch (e: JSONException) {
                    Log.e("Analytics Fetch", "Error parsing JSON response", e)
                    useFallbackData()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("Analytics Fetch", "Error fetching analytics data", e)
                useFallbackData()
            }
        })
    }

    private fun useFallbackData() {
        val random = Random()

        // Fake emotion data with random values
        emotionsData = mapOf(
            "Happy" to List(10) { random.nextFloat() * 100 },
            "Sad" to List(10) { random.nextFloat() * 100 },
            "Angry" to List(10) { random.nextFloat() * 100 },
            "Excited" to List(10) { random.nextFloat() * 100 },
            "Calm" to List(10) { random.nextFloat() * 100 },
            "Surprised" to List(10) { random.nextFloat() * 100 },
            "Fearful" to List(10) { random.nextFloat() * 100 },
            "Confused" to List(10) { random.nextFloat() * 100 },
            "Bored" to List(10) { random.nextFloat() * 100 },
            "Nostalgic" to List(10) { random.nextFloat() * 100 },
            "Grateful" to List(10) { random.nextFloat() * 100 },
            "Hopeful" to List(10) { random.nextFloat() * 100 },
            "Proud" to List(10) { random.nextFloat() * 100 },
            "Anxious" to List(10) { random.nextFloat() * 100 },
            "Content" to List(10) { random.nextFloat() * 100 }
        )


        val summaryList = listOf(
            "Exercise - Happy: Felt energized",
            "Reading - Calm: Relaxing time",
            "Work - Angry: Frustrating day"
        )

        val overallScore = random.nextDouble() * 100

        runOnUiThread {
            updateChartData()
            updateOverallScore(overallScore)
            updateSummary(summaryList)
            setupEmotionFilter()
        }
    }

    private fun setupChart() {
        val chart = binding.analyticsChart
        val xAxis = chart.xAxis

        // Configure X-Axis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.labelRotationAngle = 45f
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = 6f
        xAxis.granularity = 1f

        // Get last 7 days dynamically
        val last7Days = getLast7Days()

        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt().coerceIn(0, 6)
                return last7Days[index] // Use dynamically generated labels
            }
        }

        // Configure Y-Axis
        chart.axisRight.isEnabled = false
        chart.axisLeft.setDrawGridLines(false)
        chart.description.isEnabled = false

        // Enable touch interactions
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)

        // Configure Legend
        val legend = chart.legend
        legend.isEnabled = true
        legend.isWordWrapEnabled = true
        legend.xEntrySpace = 10f
        legend.yEntrySpace = 5f
        legend.formSize = 12f
    }

    private fun getLast7Days(): List<String> {
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault()) // e.g., "Feb 26"
        val calendar = Calendar.getInstance()
        val last7Days = mutableListOf<String>()

        for (i in 6 downTo 0) { // Last 7 days from today
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            last7Days.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, i) // Reset calendar
        }
        return last7Days
    }

    private fun updateChartData() {
        val chart = binding.analyticsChart

        if (emotionsData.isEmpty()) {
            chart.clear()
            chart.setNoDataText("No chart data available")
            return
        }

        val dataSets = mutableListOf<ILineDataSet>()
        val colors = listOf(
            Color.BLUE, Color.RED, Color.GREEN, Color.MAGENTA, Color.CYAN,
            Color.rgb(255, 165, 0),  // Orange
            Color.rgb(75, 0, 130),   // Indigo
            Color.rgb(128, 0, 128),  // Purple
            Color.rgb(0, 128, 128),  // Teal
            Color.rgb(255, 105, 180),// Hot Pink
            Color.rgb(255, 223, 0),  // Gold
            Color.rgb(0, 191, 255),  // Deep Sky Blue
            Color.rgb(34, 139, 34),  // Forest Green
            Color.rgb(255, 69, 0),   // Red-Orange
            Color.rgb(139, 0, 139)   // Dark Magenta
        )

        var index = 0
        for ((emotion, values) in emotionsData) {
            if (!selectedEmotions.contains(emotion)) continue
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

    private fun setupEmotionFilter() {
        filterButton = binding.emotionFilterButton
        filterButton.setOnClickListener {
            showEmotionSelectionDialog()
        }
    }

    private fun showEmotionSelectionDialog() {
        val emotionsArray = emotionsData.keys.toTypedArray()
        val checkedItems =
            BooleanArray(emotionsArray.size) { selectedEmotions.contains(emotionsArray[it]) }

        AlertDialog.Builder(this)
            .setTitle("Select Emotions")
            .setMultiChoiceItems(emotionsArray, checkedItems) { _, which, isChecked ->
                if (isChecked) {
                    selectedEmotions.add(emotionsArray[which])
                } else {
                    selectedEmotions.remove(emotionsArray[which])
                }
            }
            .setPositiveButton("Apply") { _, _ ->
                filterChartData() // Update chart based on selection
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun filterChartData() {
        val chart = binding.analyticsChart

        if (selectedEmotions.isEmpty()) {
            updateChartData() // Show all emotions
            return
        }

        val filteredDataSets = mutableListOf<ILineDataSet>()
        val colors = listOf(
            Color.BLUE, Color.RED, Color.GREEN, Color.MAGENTA, Color.CYAN,
            Color.rgb(255, 165, 0),  // Orange
            Color.rgb(75, 0, 130),   // Indigo
            Color.rgb(128, 0, 128),  // Purple
            Color.rgb(0, 128, 128),  // Teal
            Color.rgb(255, 105, 180),// Hot Pink
            Color.rgb(255, 223, 0),  // Gold
            Color.rgb(0, 191, 255),  // Deep Sky Blue
            Color.rgb(34, 139, 34),  // Forest Green
            Color.rgb(255, 69, 0),   // Red-Orange
            Color.rgb(139, 0, 139)   // Dark Magenta
        )

        var index = 0
        for ((emotion, values) in emotionsData) {
            if (emotion in selectedEmotions) {
                val entries = values.mapIndexed { i, value -> Entry(i.toFloat(), value) }
                val dataSet = LineDataSet(entries, emotion)
                dataSet.color = colors[index % colors.size]
                dataSet.valueTextColor = Color.BLACK
                filteredDataSets.add(dataSet)
                index++
            }
        }

        chart.data = LineData(filteredDataSets)
        chart.invalidate()
    }
}
