package com.ntando.ivu

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.data.repository.DeckRepository
import com.ntando.ivu.ui.decks.FlashcardListScreen
import com.ntando.ivu.ui.theme.IVUTheme
import com.ntando.ivu.viewmodel.FlashcardListViewModel
import com.ntando.ivu.viewmodel.DecksViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory

/**
 * [FlashcardListActivity] displays all flashcards belonging to a specific study deck.
 *
 * Intent Parameters:
 * - `deck_id` (String): Unique identifier of the selected deck (Required).
 * - `deck_title` (String): Title of the selected deck for header display (Optional, defaults to "Cards").
 *
 * Features:
 * - Hosts [FlashcardListScreen] in Jetpack Compose.
 * - Enables adding new flashcards, editing cards, or deleting the entire deck.
 * - Handles back navigation and deck removal actions.
 */
class FlashcardListActivity : ComponentActivity() {

    companion object {
        private const val TAG = "FlashcardListActivity"
    }

    private val viewModel: FlashcardListViewModel by viewModels {
        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val firebaseUid = sharedPref.getString("firebase_uid", "") ?: ""
        ViewModelFactory(FlashcardRepository(null, firebaseUid))
    }

    private val decksViewModel: DecksViewModel by viewModels {
        ViewModelFactory(DeckRepository())
    }

    /**
     * Initializes the activity, validates intent parameter presence (`deck_id`),
     * and sets up the Compose content view.
     *
     * @param savedInstanceState Saved instance bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting FlashcardListActivity")

        val deckId = intent.getStringExtra("deck_id") ?: ""
        val deckTitle = intent.getStringExtra("deck_title") ?: "Cards"

        Log.d(TAG, "Extracted intent extras - deckId: '$deckId', deckTitle: '$deckTitle'")

        if (deckId.isEmpty()) {
            Log.w(TAG, "Missing required extra 'deck_id'. Finishing activity.")
            finish()
            return
        }

        setContent {
            val preferenceManager = PreferenceManager(this)
            val isDarkTheme by preferenceManager.isDarkTheme.collectAsState(initial = false)

            IVUTheme(darkTheme = isDarkTheme) {
                FlashcardListScreen(
                    viewModel = viewModel,
                    deckId = deckId,
                    deckTitle = deckTitle,
                    onBack = {
                        Log.d(TAG, "Back clicked. Finishing activity.")
                        finish()
                    },
                    onDeleteDeck = {
                        Log.i(TAG, "Delete deck requested for deckId: '$deckId'")
                        decksViewModel.deleteDeck(deckId)
                        finish()
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
        Log.d(TAG, "onStart: FlashcardListActivity visible")
    }

    /**
     * Called before the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: FlashcardListActivity destroyed")
    }
}
