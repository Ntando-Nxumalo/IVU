package com.ntando.ivu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.repository.AchievementRepository
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.ui.review.FlashcardReviewScreen
import com.ntando.ivu.viewmodel.FlashcardReviewViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory

class ReviewActivity : ComponentActivity() {

    private val viewModel: FlashcardReviewViewModel by viewModels {
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

        val deckId = intent.getStringExtra("deck_id") ?: ""

        if (deckId.isEmpty()) {
            finish()
            return
        }

        setContent {
            FlashcardReviewScreen(
                viewModel = viewModel,
                deckId = deckId,
                onBack = { finish() }
            )
        }
    }
}
