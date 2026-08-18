package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.entity.JournalEntry
import com.ntando.ivu.data.entity.Mood
import com.ntando.ivu.data.repository.JournalRepository
import com.ntando.ivu.viewmodel.JournalViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class JournalActivity : AppCompatActivity() {

    private var currentUserId: Long = -1
    private val viewModel: JournalViewModel by viewModels {
        val db = DatabaseProvider.getDatabase(this)
        ViewModelFactory(JournalRepository(db.journalDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journal)

        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", -1)

        if (currentUserId == -1L) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setupRecyclerView()
        setupNavigation()
        
        // Refresh entries from backend
        viewModel.refreshJournalEntries(currentUserId)
    }

    private fun setupRecyclerView() {
        val rvJournal = findViewById<RecyclerView>(R.id.rvJournalEntries)
        rvJournal.layoutManager = LinearLayoutManager(this)
        
        lifecycleScope.launch {
            viewModel.getJournalEntries(currentUserId).collect { entries ->
                rvJournal.adapter = JournalAdapter(entries)
            }
        }
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, IVU::class.java))
            finish()
        }
        
        findViewById<View>(R.id.navDecks).setOnClickListener {
            startActivity(Intent(this, DecksActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.fabAddEntry).setOnClickListener {
            val intent = Intent(this, NewEntryActivity::class.java)
            startActivity(intent)
        }
    }

    inner class JournalAdapter(private val entries: List<JournalEntry>) : RecyclerView.Adapter<JournalAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val viewEntryColor: View = view.findViewById(R.id.viewEntryColor)
            val tvEntryTitle: TextView = view.findViewById(R.id.tvEntryTitle)
            val tvEntrySubtitle: TextView = view.findViewById(R.id.tvEntrySubtitle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_journal_entry, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = entries[position]
            
            val sdf = SimpleDateFormat("EEE d MMM", Locale.getDefault())
            val dateStr = sdf.format(Date(entry.date))
            
            holder.tvEntryTitle.text = "$dateStr — ${entry.text}"
            holder.tvEntrySubtitle.text = "Synced from server"

            val colorRes = when (entry.mood) {
                Mood.GREAT -> R.drawable.circle_teal
                Mood.OKAY -> R.drawable.circle_gold
                Mood.TOUGH -> R.drawable.circle_red
            }
            holder.viewEntryColor.setBackgroundResource(colorRes)
        }

        override fun getItemCount() = entries.size
    }
}
