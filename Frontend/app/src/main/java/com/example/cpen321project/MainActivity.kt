package com.example.cpen321project

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity(), CalendarAdapter.OnItemListener {
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var monthYearText: TextView
    private lateinit var selectedDate: LocalDate
    private val journalentries = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // need to get the actual date from the back end just for example
        journalentries.add("2025-02-05")
        journalentries.add("2025-02-10")
        val adddate = intent.getStringExtra("added_date") ?: ""
        journalentries.add(adddate)
        val deletedate = intent.getStringExtra("deleted_date") ?: ""
        journalentries.remove(deletedate)

        initWidgets()
        selectedDate = LocalDate.now()
        setMonthView()

        findViewById<Button>(R.id.Last_month_button).setOnClickListener(){
            selectedDate = selectedDate.minusMonths(1)
            setMonthView()
        }

        findViewById<Button>(R.id.Next_month_button).setOnClickListener(){
            selectedDate = selectedDate.plusMonths(1)
            setMonthView()
        }

        findViewById<Button>(R.id.profile_button).setOnClickListener() {
            val intent = Intent(this, ProfileManagement::class.java)
            startActivity(intent)
        }
    }

    private fun initWidgets() {
        calendarRecyclerView = findViewById(R.id.calenderrecycleView)
        monthYearText = findViewById(R.id.Year_month_TV)
    }

    private fun setMonthView() {
        monthYearText.text = monthYearFromDate(selectedDate)
        val daysInMonth = daysInMonthArray(selectedDate)

        val calendarAdapter = CalendarAdapter(daysInMonth, selectedDate, journalentries,this)
        val layoutManager = GridLayoutManager(applicationContext,7)
        calendarRecyclerView.layoutManager = layoutManager
        calendarRecyclerView.adapter = calendarAdapter
    }

    private fun daysInMonthArray(date: LocalDate): ArrayList<String> {
        val daysInMonthArray = ArrayList<String>()
        val yearMonth = YearMonth.from(date)

        val daysInMonth = yearMonth.lengthOfMonth()
        val firstofMonth = selectedDate.withDayOfMonth(1)

        val dayOfWeek = firstofMonth.dayOfWeek.value

        for(i in 1..42){
            if(i<= dayOfWeek || i> daysInMonth + dayOfWeek){
                daysInMonthArray.add("")
            } else{
                daysInMonthArray.add((i-dayOfWeek).toString())
            }
        }
        return daysInMonthArray
    }

    private fun monthYearFromDate(date: LocalDate): CharSequence? {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        return date.format(formatter)
    }

    override fun onItemClick(position: Int, dayText: String) {
        if (dayText.isNotEmpty()) {
            val message = "Selected Date $dayText ${monthYearFromDate(selectedDate)}"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            val dayNumber = dayText.toInt()
            val selectedJournalDate = selectedDate.withDayOfMonth(dayNumber)

            // Allow only past or current dates
            if (!selectedJournalDate.isAfter(LocalDate.now())) {
                val intent = Intent(this, Journal_entries::class.java)
                intent.putExtra("SELECTED_DATE", selectedJournalDate.toString()) // Pass date to next activity
                if(journalentries.contains(selectedJournalDate.toString())){ // fetch the journal entries that have been written from the database
                    val entry:String = "Fetched from database"
                    intent.putExtra("Journal_Entry_fetched", entry)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Cannot add a journal for future dates!", Toast.LENGTH_SHORT).show()
            }
        }
    }


}