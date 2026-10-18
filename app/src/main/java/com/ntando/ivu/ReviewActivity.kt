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
            db.journalDao(),
            applicationContext
        )
        ViewModelFactory(FlashcardRepository(achievementRepository, firebaseUid, this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting ReviewActivity")

        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val firebaseUid = sharedPref.getString("firebase_uid", "") ?: ""
        viewModel.setOwnerUserId(firebaseUid)

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

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: ReviewActivity visible")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ReviewActivity destroyed")
    }
}
