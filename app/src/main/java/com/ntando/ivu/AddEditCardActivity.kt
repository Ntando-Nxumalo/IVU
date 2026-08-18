package com.ntando.ivu

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.entity.Flashcard
import kotlinx.coroutines.launch

class AddEditCardActivity : AppCompatActivity() {

    private var deckId: Long = -1
    private var cardId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_card)

        deckId = intent.getLongExtra("deck_id", -1)
        cardId = intent.getLongExtra("card_id", -1)

        if (deckId == -1L && cardId == -1L) {
            finish()
            return
        }

        setupUI()
    }

    private fun setupUI() {
        val etFront = findViewById<EditText>(R.id.etFrontText)
        val etBack = findViewById<EditText>(R.id.etBackText)
        val btnSave = findViewById<Button>(R.id.btnSaveCard)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)

        if (cardId != -1L) {
            tvTitle.text = "Edit Card"
            // Load existing card data
            lifecycleScope.launch {
                val db = DatabaseProvider.getDatabase(this@AddEditCardActivity)
                val card = db.flashcardDao().getFlashcardById(cardId)
                card?.let {
                    etFront.setText(it.frontText)
                    etBack.setText(it.backText)
                }
            }
        }

        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val front = etFront.text.toString().trim()
            val back = etBack.text.toString().trim()

            if (front.isEmpty() || back.isEmpty()) {
                Toast.makeText(this, "Please fill in both sides", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val db = DatabaseProvider.getDatabase(this@AddEditCardActivity)
                if (cardId == -1L) {
                    db.flashcardDao().insertFlashcard(
                        Flashcard(deckId = deckId, frontText = front, backText = back)
                    )
                } else {
                    val existing = db.flashcardDao().getFlashcardById(cardId)
                    existing?.let {
                        db.flashcardDao().updateFlashcard(it.copy(frontText = front, backText = back))
                    }
                }
                Toast.makeText(this@AddEditCardActivity, "Card saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
