package com.ntando.ivu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.repository.AchievementRepository
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.ui.review.FlashcardReviewScreen
import com.ntando.ivu.ui.theme.IVUTheme
import com.ntando.ivu.viewmodel.FlashcardReviewViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory

class ReviewActivity : ComponentActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deckId = intent.getStringExtra("deck_id") ?: ""

        if (deckId.isEmpty()) {
            finish()
            return
        }

        setContent {
            IVUTheme {
                FlashcardReviewScreen(
                    viewModel = viewModel,
                    deckId = deckId,
                    onBack = { finish() }
                )
            }
        }
    }
}
