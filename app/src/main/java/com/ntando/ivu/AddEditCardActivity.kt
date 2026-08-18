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
import androidx.lifecycle.lifecycleScope
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.viewmodel.FlashcardViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class AddEditCardActivity : AppCompatActivity() {

    private var localDeckId: Long = -1
    private var remoteDeckId: String? = null
    private var localCardId: Long = -1

    private val viewModel: FlashcardViewModel by viewModels {
        val db = DatabaseProvider.getDatabase(this)
        ViewModelFactory(FlashcardRepository(db.flashcardDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_card)

        localDeckId = intent.getLongExtra("deck_id", -1)
        remoteDeckId = intent.getStringExtra("remote_deck_id")
        localCardId = intent.getLongExtra("card_id", -1)

        // Ensure we have enough context to either add or edit
        if (localDeckId == -1L && localCardId == -1L) {
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
        val btnDelete = findViewById<ImageButton>(R.id.btnDelete)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)

        if (localCardId != -1L) {
            // EDIT MODE
            tvTitle.text = getString(R.string.edit_card_title)
            btnDelete.visibility = View.VISIBLE
            
            lifecycleScope.launch {
                val card = viewModel.getFlashcardById(localCardId)
                card?.let {
                    etFront.setText(it.frontText)
                    etBack.setText(it.backText)
                }
            }
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

            if (localCardId == -1L) {
                // ADD: Create new card and sync to server
                if (remoteDeckId != null) {
                    viewModel.createFlashcard(remoteDeckId!!, localDeckId, front, back, null) { success ->
                        if (success) {
                            Toast.makeText(this, "Card created and synced!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            btnSave.isEnabled = true
                            Toast.makeText(this, "Failed to sync with server", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Error: Missing deck information", Toast.LENGTH_SHORT).show()
                    btnSave.isEnabled = true
                }
            } else {
                // EDIT: Update existing card locally
                lifecycleScope.launch {
                    val card = viewModel.getFlashcardById(localCardId)
                    card?.let {
                        val updated = it.copy(frontText = front, backText = back)
                        viewModel.updateFlashcardLocal(updated)
                        Toast.makeText(this@AddEditCardActivity, "Card updated locally", Toast.LENGTH_SHORT).show()
                        finish()
                    } ?: run {
                        btnSave.isEnabled = true
                        Toast.makeText(this@AddEditCardActivity, "Error: Card not found", Toast.LENGTH_SHORT).show()
                    }
                }
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
        lifecycleScope.launch {
            val card = viewModel.getFlashcardById(localCardId)
            card?.let {
                viewModel.deleteCard(remoteDeckId, it.remoteId, it) { success ->
                    if (success) {
                        Toast.makeText(this@AddEditCardActivity, "Card deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AddEditCardActivity, "Failed to delete from server", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
