package com.ntando.ivu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.ui.decks.FlashcardListScreen
import com.ntando.ivu.viewmodel.FlashcardListViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory

class FlashcardListActivity : ComponentActivity() {

    private val viewModel: FlashcardListViewModel by viewModels {
        ViewModelFactory(FlashcardRepository())
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
            FlashcardListScreen(
                viewModel = viewModel,
                deckId = deckId,
                deckTitle = deckTitle,
                onBack = { finish() }
            )
        }
    }
}
