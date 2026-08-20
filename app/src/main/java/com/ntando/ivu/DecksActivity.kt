package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.repository.AchievementRepository
import com.ntando.ivu.data.repository.DeckRepository
import com.ntando.ivu.ui.decks.DecksScreen
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
        val currentUserId = sharedPref.getLong("current_user_id", -1)
        val db = DatabaseProvider.getDatabase(this)
        val achievementRepository = AchievementRepository(
            db.achievementDao(),
            db.userStatsDao(),
            db.journalDao(),
            db.flashcardDao()
        )
        ViewModelFactory(FlashcardRepository(achievementRepository, currentUserId))
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
                }
            )
        }
    }
}
