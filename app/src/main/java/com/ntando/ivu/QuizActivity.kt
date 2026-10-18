package com.ntando.ivu

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.firebase.auth.FirebaseAuth
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.data.repository.AchievementRepository
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.ui.quiz.QuizScreen
import com.ntando.ivu.ui.theme.IVUTheme
import com.ntando.ivu.viewmodel.QuizViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory

/**
 * [QuizActivity] hosts the Quiz Mode feature for a selected study deck.
 */
class QuizActivity : ComponentActivity() {

    companion object {
        private const val TAG = "IVU_QUIZ"
    }

    private val viewModel: QuizViewModel by viewModels {
        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val firebaseUid = sharedPref.getString("firebase_uid", "") ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val db = DatabaseProvider.getDatabase(this)
        val achievementRepository = AchievementRepository(
            db.userStatsDao(),
            db.journalDao(),
            applicationContext
        )
        val flashcardRepository = FlashcardRepository(achievementRepository, firebaseUid, this)
        ViewModelFactory(flashcardRepository to achievementRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing QuizActivity")

        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val firebaseUid = sharedPref.getString("firebase_uid", "") ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val deckId = intent.getLongExtra("deck_id", -1L)
        val deckTitle = intent.getStringExtra("deck_title") ?: "Quiz"

        Log.d(TAG, "QuizActivity intent extras - deckId: $deckId, deckTitle: '$deckTitle', firebaseUid: '$firebaseUid'")

        if (deckId == -1L) {
            Log.w(TAG, "Invalid deck_id passed to QuizActivity. Finishing.")
            finish()
            return
        }

        viewModel.initDeck(deckId, deckTitle, firebaseUid)

        setContent {
            val preferenceManager = PreferenceManager(this)
            val isDarkTheme by preferenceManager.isDarkTheme.collectAsState(initial = false)

            IVUTheme(darkTheme = isDarkTheme) {
                QuizScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}
