package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.data.repository.AchievementRepository
import com.ntando.ivu.data.repository.DeckRepository
import com.ntando.ivu.ui.decks.DecksScreen
import com.ntando.ivu.ui.theme.IVUTheme
import com.ntando.ivu.viewmodel.DecksViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.viewmodel.FlashcardViewModel

class DecksActivity : ComponentActivity() {

    private val viewModel: DecksViewModel by viewModels {
        ViewModelFactory(DeckRepository())
    }
    
    private val flashcardViewModel: FlashcardViewModel by viewModels {
        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val firebaseUid = sharedPref.getString("firebase_uid", "") ?: ""
        val db = DatabaseProvider.getDatabase(this)
        val achievementRepository = AchievementRepository(
            db.userStatsDao(),
            db.journalDao()
        )
        ViewModelFactory(FlashcardRepository(achievementRepository, firebaseUid))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val currentUserId = sharedPref.getLong("current_user_id", -1)

        if (currentUserId == -1L) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            val preferenceManager = PreferenceManager(this)
            val isDarkTheme by preferenceManager.isDarkTheme.collectAsState(initial = false)
            
            IVUTheme(darkTheme = isDarkTheme) {
                DecksScreen(
                    viewModel = viewModel,
                    flashcardViewModel = flashcardViewModel,
                    onDeckClick = { deckId ->
                        val intent = Intent(this, ReviewActivity::class.java).apply {
                            putExtra("deck_id", deckId)
                        }
                        startActivity(intent)
                    },
                    onViewCards = { deckId, deckTitle ->
                        val intent = Intent(this, FlashcardListActivity::class.java).apply {
                            putExtra("deck_id", deckId)
                            putExtra("deck_title", deckTitle)
                        }
                        startActivity(intent)
                    },
                    onNavigate = { screen ->
                        when (screen) {
                            "home" -> startActivity(Intent(this, IVU::class.java))
                            "decks" -> {} // Already here
                            "journal" -> startActivity(Intent(this, JournalActivity::class.java))
                            "profile" -> startActivity(Intent(this, AchievementsActivity::class.java))
                        }
                    }
                )
            }
        }
    }
}
