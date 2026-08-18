package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.entity.Deck
import com.ntando.ivu.data.entity.Language
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DecksActivity : AppCompatActivity() {

    private var currentUserId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decks)

        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", -1)

        if (currentUserId == -1L) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setupRecyclerView()
        setupNavigation()
    }

    private fun setupRecyclerView() {
        val rvDecks = findViewById<RecyclerView>(R.id.rvDecks)
        rvDecks.layoutManager = LinearLayoutManager(this)
        
        val db = DatabaseProvider.getDatabase(this)
        lifecycleScope.launch {
            db.deckDao().getDecksByUser(currentUserId).collect { decks ->
                rvDecks.adapter = DecksAdapter(decks)
            }
        }
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, IVU::class.java))
            finish()
        }
        
        findViewById<View>(R.id.navJournal).setOnClickListener {
            startActivity(Intent(this, JournalActivity::class.java))
            finish()
        }
        
        findViewById<View>(R.id.navMe).setOnClickListener {
            startActivity(Intent(this, AchievementsActivity::class.java))
            finish()
        }
        
        findViewById<View>(R.id.fabAddDeck).setOnClickListener {
            // Logic to add a new deck
        }
    }

    inner class DecksAdapter(private val decks: List<Deck>) : RecyclerView.Adapter<DecksAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvLanguageTag: TextView = view.findViewById(R.id.tvLanguageTag)
            val tvDeckTitle: TextView = view.findViewById(R.id.tvDeckTitle)
            val pbMastery: ProgressBar = view.findViewById(R.id.pbMastery)
            val tvMasteryCount: TextView = view.findViewById(R.id.tvMasteryCount)

            init {
                view.setOnClickListener {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        val deck = decks[bindingAdapterPosition]
                        val intent = Intent(this@DecksActivity, ReviewActivity::class.java).apply {
                            putExtra("deck_id", deck.deckId)
                        }
                        startActivity(intent)
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_deck, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val deck = decks[position]
            holder.tvDeckTitle.text = deck.title
            
            when (deck.language) {
                Language.ZU -> {
                    holder.tvLanguageTag.text = "isiZulu"
                    holder.tvLanguageTag.setBackgroundResource(R.drawable.bg_tag_red)
                }
                Language.AF -> {
                    holder.tvLanguageTag.text = "Afrikaans"
                    holder.tvLanguageTag.setBackgroundResource(R.drawable.bg_tag_teal)
                }
                Language.EN -> {
                    holder.tvLanguageTag.text = "English"
                    holder.tvLanguageTag.setBackgroundResource(R.drawable.bg_tag_gold)
                }
            }
            
            // Apply mock data matching the user's provided image
            val progress = when (position) {
                0 -> 72 // Everyday isiZulu
                1 -> 35 // Afrikaans Basics
                2 -> 90 // Exam Vocabulary
                3 -> 15 // Travel Phrases
                else -> 0
            }
            val masteredCount = when (position) {
                0 -> 29
                1 -> 14
                2 -> 36
                3 -> 6
                else -> 0
            }
            
            holder.pbMastery.progress = progress
            holder.tvMasteryCount.text = "$masteredCount / 40 cards mastered"
        }

        override fun getItemCount() = decks.size
    }
}
