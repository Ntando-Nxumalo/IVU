package com.ntando.ivu

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.entity.JournalEntry
import com.ntando.ivu.data.entity.Mood
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NewEntryActivity : AppCompatActivity() {

    private var currentUserId: Long = -1
    private var selectedMood: Mood = Mood.OKAY

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
        tvDate.text = sdf.format(Date())

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

            lifecycleScope.launch {
                val db = DatabaseProvider.getDatabase(this@NewEntryActivity)
                db.journalDao().insertEntry(
                    JournalEntry(
                        userId = currentUserId,
                        mood = selectedMood,
                        text = notes,
                        date = System.currentTimeMillis()
                    )
                )
                Toast.makeText(this@NewEntryActivity, "Entry saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
