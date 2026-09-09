package com.ntando.ivu

import android.os.Bundle
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

class FlashcardListActivity : ComponentActivity() {

    private val viewModel: FlashcardListViewModel by viewModels {
        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val firebaseUid = sharedPref.getString("firebase_uid", "") ?: ""
        ViewModelFactory(FlashcardRepository(null, firebaseUid))
    }

    private val decksViewModel: DecksViewModel by viewModels {
        ViewModelFactory(DeckRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deckId = intent.getStringExtra("deck_id") ?: ""
        val deckTitle = intent.getStringExtra("deck_title") ?: "Cards"

        if (deckId.isEmpty()) {
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
                    onBack = { finish() },
                    onDeleteDeck = {
                        decksViewModel.deleteDeck(deckId)
                        finish()
                    }
                )
            }
        }
    }
}
