package com.ntando.ivu

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.entity.Mood
import com.ntando.ivu.data.repository.JournalRepository
import com.ntando.ivu.viewmodel.JournalViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

class NewEntryActivity : AppCompatActivity() {

    private var currentUserId: Long = -1
    private var selectedMood: Mood = Mood.OKAY

    private val viewModel: JournalViewModel by viewModels {
        val db = DatabaseProvider.getDatabase(this)
        ViewModelFactory(JournalRepository(db.journalDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_journal_entry)

        val sharedPref = getSharedPreferences("IVUPrefs", Context.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", -1)

        if (currentUserId == -1L) {
            finish()
            return
        }

        setupUI()
    }

    private fun setupUI() {
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etNotes = findViewById<EditText>(R.id.etNotes)
        val btnSave = findViewById<Button>(R.id.btnSaveEntry)

        // Set Current Date
        val sdf = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        val isoSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()) // Example ISO format
        val currentDate = Date()
        tvDate.text = sdf.format(currentDate)

        btnBack.setOnClickListener { finish() }

        findViewById<android.view.View>(R.id.btnMoodGreat).setOnClickListener {
            selectedMood = Mood.GREAT
            Toast.makeText(this, "Selected: Great", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.btnMoodOkay).setOnClickListener {
            selectedMood = Mood.OKAY
            Toast.makeText(this, "Selected: Okay", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.btnMoodTough).setOnClickListener {
            selectedMood = Mood.TOUGH
            Toast.makeText(this, "Selected: Tough", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            val notes = etNotes.text.toString().trim()
            if (notes.isEmpty()) {
                Toast.makeText(this, "Please write some notes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false // Prevent double clicks
            
            viewModel.createJournalEntry(
                userId = currentUserId,
                date = isoSdf.format(currentDate),
                mood = selectedMood.name.lowercase(),
                text = notes,
                deckId = null // Can be linked to a specific deck later
            ) { success ->
                if (success) {
                    Toast.makeText(this, "Entry saved and synced!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Failed to sync with server. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
