package com.example.cpen321project

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Journal_entries : AppCompatActivity() {

    private lateinit var journaldatetext: TextView
    private lateinit var journalentrytext: EditText
    private lateinit var backtomainpage: Button
    private lateinit var editentry: ImageButton
    private lateinit var deleteentry: ImageButton
    private lateinit var share_entry: ImageButton
    private lateinit var add_image: ImageButton
    private lateinit var save_entry: Button
    private var selectedDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_journal_entries)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.journalview)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        journaldatetext = findViewById(R.id.journalDateText)
        journalentrytext = findViewById(R.id.journalEntryInput)
        backtomainpage = findViewById(R.id.Backbuttonentries)
        editentry = findViewById(R.id.editbutton)
        deleteentry = findViewById(R.id.deletebutton)
        share_entry = findViewById(R.id.sharebutton)
        add_image = findViewById(R.id.addimageButton)
        save_entry = findViewById(R.id.Saveentrybutton)

        selectedDate = intent.getStringExtra("SELECTED_DATE") ?: ""
        journaldatetext.text = "Journal Entry for $selectedDate"

        val entry = intent.getStringExtra("Journal_Entry_fetched") ?: ""
        journalentrytext.setText(entry)
        journalentrytext.isEnabled = false

        backtomainpage.setOnClickListener(){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        editentry.setOnClickListener(){
            journalentrytext.isEnabled = true
            journalentrytext.requestFocus()
        }

        deleteentry.setOnClickListener(){
            showdeleteconformationpopup()
        }

        save_entry.setOnClickListener(){
            if(journalentrytext.text.toString().isNotEmpty() && selectedDate != null){
                saveentry()
            }
        }
    }

    private fun saveentry() { // Need to save the entry to the database
        Toast.makeText(this, "Journal deleted for $selectedDate", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("added_date",selectedDate.toString())
        startActivity(intent)
        finish()
    }

    private fun showdeleteconformationpopup() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Delete Journal Entry")
            .setMessage("Are you sure you want to delete this journal entry?")
            .setPositiveButton("Yes") { _, _ ->
                deleteJournalEntry()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss() // Dismiss dialog, do nothing
            }
            .create()

        alertDialog.show()
    }

    private fun deleteJournalEntry() {
        if (journalentrytext.text.toString().isNotEmpty() && selectedDate != null) {
            journalentrytext.setText("")
            Toast.makeText(this, "Journal deleted for $selectedDate", Toast.LENGTH_SHORT).show()
            // Close activity and go back to main screen
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("deleted_date",selectedDate.toString())
            startActivity(intent)
            finish()
        }
    }
}