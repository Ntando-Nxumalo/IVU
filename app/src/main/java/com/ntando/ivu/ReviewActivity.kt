package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.entity.Flashcard
import com.ntando.ivu.data.repository.DeckRepository
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.viewmodel.FlashcardViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class ReviewActivity : AppCompatActivity() {

    private var currentUserId: Long = -1
    private var localDeckId: Long = -1
    private var remoteDeckId: String? = null
    private var flashcards: List<Flashcard> = emptyList()
    private var currentIndex = 0
    private var isShowingAnswer = false

    private lateinit var tvMainText: TextView
    private lateinit var tvSubText: TextView
    private lateinit var tvHint: TextView
    private lateinit var tvProgress: TextView
    private lateinit var pbReview: ProgressBar
    private lateinit var ratingBar: View
    private lateinit var cvFlashcard: View
    private lateinit var btnEdit: ImageButton

    private val viewModel: FlashcardViewModel by viewModels {
        val db = DatabaseProvider.getDatabase(this)
        ViewModelFactory(FlashcardRepository(db.flashcardDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", -1)
        localDeckId = intent.getLongExtra("deck_id", -1)

        if (currentUserId == -1L || localDeckId == -1L) {
            finish()
            return
        }

        initViews()
        loadDeckAndFlashcards()
    }

    private fun initViews() {
        tvMainText = findViewById(R.id.tvMainText)
        tvSubText = findViewById(R.id.tvSubText)
        tvHint = findViewById(R.id.tvHint)
        tvProgress = findViewById(R.id.tvProgress)
        pbReview = findViewById(R.id.pbReview)
        ratingBar = findViewById(R.id.ratingBar)
        cvFlashcard = findViewById(R.id.cvFlashcard)
        btnEdit = findViewById(R.id.btnEditCard)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        cvFlashcard.setOnClickListener {
            if (!isShowingAnswer) {
                showAnswer()
            }
        }

        btnEdit.setOnClickListener {
            val card = flashcards[currentIndex]
            val intent = Intent(this, AddEditCardActivity::class.java).apply {
                putExtra("card_id", card.cardId)
                putExtra("deck_id", localDeckId)
                putExtra("remote_deck_id", remoteDeckId)
            }
            startActivity(intent)
        }

        ratingBar.visibility = View.INVISIBLE

        findViewById<Button>(R.id.btnAgain).setOnClickListener { submitReview("again") }
        findViewById<Button>(R.id.btnHard).setOnClickListener { submitReview("hard") }
        findViewById<Button>(R.id.btnGood).setOnClickListener { submitReview("good") }
        findViewById<Button>(R.id.btnEasy).setOnClickListener { submitReview("easy") }
    }

    private fun loadDeckAndFlashcards() {
        val db = DatabaseProvider.getDatabase(this)
        lifecycleScope.launch {
            val deck = db.deckDao().getDeckById(localDeckId)
            remoteDeckId = deck?.remoteId
            
            // Refresh from server if we have a remote ID
            remoteDeckId?.let { 
                viewModel.refreshFlashcards(it, localDeckId)
            }

            viewModel.getFlashcards(localDeckId).collect { list ->
                flashcards = list
                if (flashcards.isNotEmpty()) {
                    if (currentIndex >= flashcards.size) currentIndex = 0
                    updateUI()
                } else {
                    // Offer to add a card if empty
                    val intent = Intent(this@ReviewActivity, AddEditCardActivity::class.java).apply {
                        putExtra("deck_id", localDeckId)
                        putExtra("remote_deck_id", remoteDeckId)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun updateUI() {
        val card = flashcards[currentIndex]
        tvMainText.text = card.frontText
        tvSubText.text = "" // Could show language here
        tvHint.text = "Tap card to reveal answer"
        tvHint.visibility = View.VISIBLE
        ratingBar.visibility = View.INVISIBLE
        isShowingAnswer = false

        tvProgress.text = "${currentIndex + 1}/${flashcards.size}"
        pbReview.max = flashcards.size
        pbReview.progress = currentIndex + 1
    }

    private fun showAnswer() {
        val card = flashcards[currentIndex]
        tvMainText.text = card.backText
        tvHint.visibility = View.GONE
        ratingBar.visibility = View.VISIBLE
        isShowingAnswer = true
    }

    private fun submitReview(rating: String) {
        val card = flashcards[currentIndex]
        val remoteCardId = card.remoteId

        if (remoteDeckId != null && remoteCardId != null) {
            viewModel.reviewCard(remoteDeckId!!, remoteCardId, card, rating) { success ->
                if (!success) {
                    Toast.makeText(this, "Failed to sync review with server", Toast.LENGTH_SHORT).show()
                }
                nextCard()
            }
        } else {
            // Local only fallback or error
            nextCard()
        }
    }

    private fun nextCard() {
        currentIndex++
        if (currentIndex < flashcards.size) {
            updateUI()
        } else {
            Toast.makeText(this, "Review session finished!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
