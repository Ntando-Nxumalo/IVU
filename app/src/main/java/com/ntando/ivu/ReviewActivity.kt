package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.entity.Flashcard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReviewActivity : AppCompatActivity() {

    private var currentUserId: Long = -1
    private var deckId: Long = -1
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", -1)
        deckId = intent.getLongExtra("deck_id", -1)

        if (currentUserId == -1L || deckId == -1L) {
            finish()
            return
        }

        initViews()
        loadFlashcards()
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
                putExtra("deck_id", deckId)
            }
            startActivity(intent)
        }

        ratingBar.visibility = View.INVISIBLE

        findViewById<Button>(R.id.btnAgain).setOnClickListener { nextCard() }
        findViewById<Button>(R.id.btnHard).setOnClickListener { nextCard() }
        findViewById<Button>(R.id.btnGood).setOnClickListener { nextCard() }
        findViewById<Button>(R.id.btnEasy).setOnClickListener { nextCard() }
    }

    private fun loadFlashcards() {
        val db = DatabaseProvider.getDatabase(this)
        lifecycleScope.launch {
            db.flashcardDao().getFlashcardsByDeck(deckId).collect { list ->
                flashcards = list
                if (flashcards.isNotEmpty() && currentIndex < flashcards.size) {
                    updateUI()
                } else if (flashcards.isEmpty()) {
                    // Offer to add a card if empty
                    val intent = Intent(this@ReviewActivity, AddEditCardActivity::class.java).apply {
                        putExtra("deck_id", deckId)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    finish()
                }
            }
        }
    }

    private fun updateUI() {
        val card = flashcards[currentIndex]
        tvMainText.text = card.frontText
        tvSubText.text = "(isiZulu)" 
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

    private fun nextCard() {
        currentIndex++
        if (currentIndex < flashcards.size) {
            updateUI()
        } else {
            finish()
        }
    }
}
