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
import com.ntando.ivu.ui.decks.DecksScreen
import com.ntando.ivu.ui.theme.IVUTheme
import com.ntando.ivu.viewmodel.DecksViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.viewmodel.FlashcardViewModel

/**
 * [DecksActivity] manages and displays flashcard study decks for the current user.
 *
 * Features:
 * - Verifies user session (`current_user_id`) before rendering content. Redirects unauthenticated users to [MainActivity].
 * - Hosts [DecksScreen] written in Jetpack Compose.
 * - Integrates [DecksViewModel] for deck management and [FlashcardViewModel] for flashcard updates.
 * - Facilitates navigation to [ReviewActivity] for deck review sessions and [FlashcardListActivity] for deck editing.
 * - Handles bottom navigation bar routing across IVU sections.
 */
class DecksActivity : ComponentActivity() {

    companion object {
        private const val TAG = "DecksActivity"
    }

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

    /**
     * Initializes the activity lifecycle, validates user session, configures ViewModels,
     * and sets up the Jetpack Compose UI content.
     *
     * @param savedInstanceState Saved instance bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting DecksActivity")

        // Verify session
        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val currentUserId = sharedPref.getLong("current_user_id", -1)
        Log.d(TAG, "Extracted current_user_id: $currentUserId")

        if (currentUserId == -1L) {
            Log.w(TAG, "No valid user session found. Redirecting to MainActivity.")
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

    /**
     * Called when the activity becomes visible.
     */
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: DecksActivity visible")
    }

    /**
     * Called before the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: DecksActivity destroyed")
    }
}
