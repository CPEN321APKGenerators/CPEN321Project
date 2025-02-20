package com.example.cpen321project

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate

class CalendarAdapter (private val dayOfMonth: ArrayList<String>,private val selectedDate:LocalDate,private val journalEntries: Set<String>, private val onItemListener: OnItemListener): RecyclerView.Adapter<CalendarViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.calendar_cell, parent,false)

        val layoutParams = view.layoutParams
        layoutParams.height = (parent.height * 0.14).toInt()

        return  CalendarViewHolder(view, onItemListener)
    }

    override fun getItemCount(): Int {
        return dayOfMonth.size
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val dayText = dayOfMonth[position]
        holder.dayOfMonth.text = dayText

        // Set default color
//        holder.dayOfMonth.setBackgroundColor(Color.WHITE)

        if (dayText.isNotEmpty()) {
            val dayNumber = dayText.toInt()
            val currentDate = selectedDate.withDayOfMonth(dayNumber)
            val dateKey = currentDate.toString() // Format: YYYY-MM-DD


            when {
                journalEntries.contains(dateKey) -> {
                    // Highlight dates with journals (e.g., blue)
                    holder.dayOfMonth.setBackgroundResource(R.drawable.circle_background)
                    holder.dayOfMonth.setTextColor(Color.BLACK)
                }
                currentDate.isAfter(LocalDate.now()) -> {
                    // Grey out future dates
                    holder.dayOfMonth.setBackgroundColor(Color.TRANSPARENT)
                    holder.dayOfMonth.setTextColor(Color.GRAY)
                }
                else -> {
                    // Default color for past dates without journals
                    holder.dayOfMonth.setBackgroundColor(Color.TRANSPARENT)
                    holder.dayOfMonth.setTextColor(Color.BLACK)
                }
            }

            holder.itemView.setOnClickListener {
                if (!currentDate.isAfter(LocalDate.now())) { // Allow clicks only on past and current dates
                    onItemListener.onItemClick(position, dayText)
                }
            }
        }
    }

    interface OnItemListener{
        fun onItemClick(position: Int, dayText: String)
    }

}