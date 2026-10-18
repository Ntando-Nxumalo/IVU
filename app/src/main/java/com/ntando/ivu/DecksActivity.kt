package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.data.repository.AchievementRepository
import com.ntando.ivu.data.repository.DeckRepository
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.ui.decks.DecksScreen
import com.ntando.ivu.ui.theme.IVUTheme
import com.ntando.ivu.viewmodel.DecksViewModel
import com.ntando.ivu.viewmodel.FlashcardViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory

/**
 * [DecksActivity] manages and displays flashcard study decks for the current user.
 */
class DecksActivity : ComponentActivity() {

    companion object {
        private const val TAG = "DecksActivity"
    }

    private val viewModel: DecksViewModel by viewModels {
        ViewModelFactory(DeckRepository(this))
    }
    
    private val flashcardViewModel: FlashcardViewModel by viewModels {
        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val firebaseUid = sharedPref.getString("firebase_uid", "") ?: ""
        val db = DatabaseProvider.getDatabase(this)
        val achievementRepository = AchievementRepository(
            db.userStatsDao(),
            db.journalDao(),
            applicationContext
        )
        ViewModelFactory(FlashcardRepository(achievementRepository, firebaseUid, this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting DecksActivity")

        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val currentUserId = sharedPref.getLong("current_user_id", -1)
        val firebaseUid = sharedPref.getString("firebase_uid", "") ?: ""
        Log.d(TAG, "Extracted current_user_id: $currentUserId, firebaseUid: $firebaseUid")

        if (currentUserId == -1L) {
            Log.w(TAG, "No valid user session found. Redirecting to MainActivity.")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        viewModel.setOwnerUserId(firebaseUid)
        flashcardViewModel.setOwnerUserId(firebaseUid)

        setContent {
            val preferenceManager = PreferenceManager(this)
            val isDarkTheme by preferenceManager.isDarkTheme.collectAsState(initial = false)
            
            IVUTheme(darkTheme = isDarkTheme) {
                DecksScreen(
                    viewModel = viewModel,
                    flashcardViewModel = flashcardViewModel,
                    onDeckClick = { deckId ->
                        Log.i(TAG, "Deck selected for review - deckId: $deckId")
                        val intent = Intent(this, ReviewActivity::class.java).apply {
                            putExtra("deck_id", deckId)
                        }
                        startActivity(intent)
                    },
                    onViewCards = { deckId, deckTitle ->
                        Log.i(TAG, "View cards requested - deckId: $deckId, deckTitle: $deckTitle")
                        val intent = Intent(this, FlashcardListActivity::class.java).apply {
                            putExtra("deck_id", deckId)
                            putExtra("deck_title", deckTitle)
                        }
                        startActivity(intent)
                    },
                    onStartQuiz = { deckId, deckTitle ->
                        Log.i(TAG, "Start quiz requested - deckId: $deckId, deckTitle: $deckTitle")
                        val intent = Intent(this, QuizActivity::class.java).apply {
                            putExtra("deck_id", deckId)
                            putExtra("deck_title", deckTitle)
                        }
                        startActivity(intent)
                    },
                    onNavigate = { screen ->
                        Log.i(TAG, "Navigation requested to screen: '$screen'")
                        when (screen) {
                            "home" -> startActivity(Intent(this, IVU::class.java))
                            "decks" -> Log.d(TAG, "Already on decks screen")
                            "journal" -> startActivity(Intent(this, JournalActivity::class.java))
                            "profile" -> startActivity(Intent(this, AchievementsActivity::class.java))
                        }
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: DecksActivity visible")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: DecksActivity destroyed")
    }
}
