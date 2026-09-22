package com.ntando.ivu

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
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.ui.review.FlashcardReviewScreen
import com.ntando.ivu.ui.theme.IVUTheme
import com.ntando.ivu.viewmodel.FlashcardReviewViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory

/**
 * [ReviewActivity] provides an interactive flashcard review session for a specific deck.
 *
 * Intent Parameters:
 * - `deck_id` (String): The ID of the deck to review (Required).
 *
 * Features:
 * - Hosts [FlashcardReviewScreen] in Jetpack Compose.
 * - Tracks user review answers, updates streak/XP stats upon completion via [AchievementRepository].
 * - Supports theme configuration from [PreferenceManager].
 */
class ReviewActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ReviewActivity"
    }

    private val viewModel: FlashcardReviewViewModel by viewModels {
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
     * Initializes activity lifecycle, validates intent parameter (`deck_id`),
     * and sets up the Compose review screen.
     *
     * @param savedInstanceState Saved instance bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting ReviewActivity")

        val deckId = intent.getStringExtra("deck_id") ?: ""
        Log.d(TAG, "Extracted intent parameter - deck_id: '$deckId'")

        if (deckId.isEmpty()) {
            Log.w(TAG, "Missing required extra 'deck_id'. Finishing activity.")
            finish()
            return
        }

        setContent {
            val preferenceManager = PreferenceManager(this)
            val isDarkTheme by preferenceManager.isDarkTheme.collectAsState(initial = false)

            IVUTheme(darkTheme = isDarkTheme) {
                FlashcardReviewScreen(
                    viewModel = viewModel,
                    deckId = deckId,
                    onBack = {
                        Log.d(TAG, "Back action triggered from review screen. Finishing activity.")
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
        Log.d(TAG, "onStart: ReviewActivity visible")
    }

    /**
     * Called before the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ReviewActivity destroyed")
    }
}
