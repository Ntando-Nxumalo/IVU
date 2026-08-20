package com.ntando.ivu

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.viewmodel.FlashcardViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory

class AddEditCardActivity : AppCompatActivity() {

    private var remoteDeckId: String? = null
    private var remoteCardId: String? = null

    private val viewModel: FlashcardViewModel by viewModels {
        ViewModelFactory(FlashcardRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_card)

        remoteDeckId = intent.getStringExtra("remote_deck_id")
        remoteCardId = intent.getStringExtra("card_id")

        setupUI()
    }

    private fun setupUI() {
        val etFront = findViewById<EditText>(R.id.etFrontText)
        val etBack = findViewById<EditText>(R.id.etBackText)
        val btnSave = findViewById<Button>(R.id.btnSaveCard)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnDelete = findViewById<ImageButton>(R.id.btnDelete)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)

        if (remoteCardId != null) {
            // EDIT MODE
            tvTitle.text = getString(R.string.edit_card_title)
            btnDelete.visibility = View.VISIBLE
        } else {
            // ADD MODE
            tvTitle.text = getString(R.string.add_card_title)
            btnDelete.visibility = View.GONE
        }

        btnBack.setOnClickListener { finish() }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        btnSave.setOnClickListener {
            val front = etFront.text.toString().trim()
            val back = etBack.text.toString().trim()

            if (front.isEmpty() || back.isEmpty()) {
                Toast.makeText(this, R.string.error_fill_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false

            if (remoteCardId == null) {
                // ADD
                remoteDeckId?.let { deckId ->
                    viewModel.createFlashcard(deckId, front, back) { success ->
                        if (success) {
                            Toast.makeText(this, "Card created!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            btnSave.isEnabled = true
                            Toast.makeText(this, "Failed to create card", Toast.LENGTH_SHORT).show()
                        }
                    }
                } ?: run {
                    Toast.makeText(this, "Error: Missing deck ID", Toast.LENGTH_SHORT).show()
                    btnSave.isEnabled = true
                }
            } else {
                // EDIT: Placeholder
                Toast.makeText(this, "Edit not implemented yet", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
            }
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_card_title))
            .setMessage(getString(R.string.delete_card_confirm_msg))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteCard()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteCard() {
        val deckId = remoteDeckId
        val cardId = remoteCardId
        if (deckId != null && cardId != null) {
            viewModel.deleteFlashcard(deckId, cardId) { success ->
                if (success) {
                    Toast.makeText(this, "Card deleted", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to delete card", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
