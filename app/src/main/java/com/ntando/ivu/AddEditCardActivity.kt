package com.ntando.ivu

import android.os.Bundle
import android.util.Log
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

/**
 * [AddEditCardActivity] provides a traditional XML View-based UI for creating, editing,
 * or deleting individual flashcards within a specified deck.
 *
 * Intent Extras:
 * - `remote_deck_id` (String): ID of the deck to which the flashcard belongs or will be added.
 * - `card_id` (String?): ID of the flashcard if in Edit Mode; null if creating a new card.
 *
 * Features:
 * - Form validation for front and back card text fields.
 * - Alert dialog confirmation prior to flashcard deletion.
 * - Integration with [FlashcardViewModel] for remote and local data management.
 */
class AddEditCardActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AddEditCardActivity"
    }

    private var remoteDeckId: String? = null
    private var remoteCardId: String? = null

    private val viewModel: FlashcardViewModel by viewModels {
        ViewModelFactory(FlashcardRepository())
    }

    /**
     * Called when the activity is created. Extracts intent extras and initializes the UI components.
     *
     * @param savedInstanceState Saved instance state bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting AddEditCardActivity")
        setContentView(R.layout.activity_add_edit_card)

        // Extract intent parameters
        remoteDeckId = intent.getStringExtra("remote_deck_id")
        remoteCardId = intent.getStringExtra("card_id")

        Log.d(TAG, "Extracted intent parameters - remoteDeckId: $remoteDeckId, remoteCardId: $remoteCardId")

        setupUI()
    }

    /**
     * Called when the activity becomes visible.
     */
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: AddEditCardActivity visible")
    }

    /**
     * Called before the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: AddEditCardActivity destroyed")
    }

    /**
     * Binds UI widgets from XML layout, configures title and delete button visibility based on mode
     * (Add vs. Edit), and attaches user interaction click listeners.
     */
    private fun setupUI() {
        Log.d(TAG, "setupUI: Binding view references and setting up click listeners")
        val etFront = findViewById<EditText>(R.id.etFrontText)
        val etBack = findViewById<EditText>(R.id.etBackText)
        val btnSave = findViewById<Button>(R.id.btnSaveCard)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnDelete = findViewById<ImageButton>(R.id.btnDelete)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)

        // Configure mode UI (Add vs Edit)
        if (remoteCardId != null) {
            Log.i(TAG, "Configuring UI for EDIT mode (cardId: $remoteCardId)")
            tvTitle.text = getString(R.string.edit_card_title)
            btnDelete.visibility = View.VISIBLE
        } else {
            Log.i(TAG, "Configuring UI for ADD mode")
            tvTitle.text = getString(R.string.add_card_title)
            btnDelete.visibility = View.GONE
        }

        // Back button action listener
        btnBack.setOnClickListener {
            Log.d(TAG, "Back button clicked. Finishing activity.")
            finish()
        }

        // Delete button action listener
        btnDelete.setOnClickListener {
            Log.d(TAG, "Delete button clicked. Showing delete confirmation dialog.")
            showDeleteConfirmation()
        }

        // Save button action listener
        btnSave.setOnClickListener {
            val front = etFront.text.toString().trim()
            val back = etBack.text.toString().trim()

            Log.d(TAG, "Save card clicked. Front text length: ${front.length}, Back text length: ${back.length}")

            // Validate form input
            if (front.isEmpty() || back.isEmpty()) {
                Log.w(TAG, "Validation failed: Front or Back text is empty")
                Toast.makeText(this, R.string.error_fill_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false

            if (remoteCardId == null) {
                // ADD CARD MODE
                remoteDeckId?.let { deckId ->
                    Log.i(TAG, "Attempting to create flashcard in deck $deckId")
                    viewModel.createFlashcard(deckId, front, back) { success ->
                        if (success) {
                            Log.i(TAG, "Flashcard created successfully")
                            Toast.makeText(this, "Card created!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Log.e(TAG, "Failed to create flashcard in deck $deckId")
                            btnSave.isEnabled = true
                            Toast.makeText(this, "Failed to create card", Toast.LENGTH_SHORT).show()
                        }
                    }
                } ?: run {
                    Log.e(TAG, "Error: missing remoteDeckId when saving card")
                    Toast.makeText(this, "Error: Missing deck ID", Toast.LENGTH_SHORT).show()
                    btnSave.isEnabled = true
                }
            } else {
                // EDIT CARD MODE
                Log.i(TAG, "Edit flashcard requested (placeholder implementation)")
                Toast.makeText(this, "Edit not implemented yet", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
            }
        }
    }

    /**
     * Displays an [AlertDialog] asking the user to confirm card deletion.
     */
    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_card_title))
            .setMessage(getString(R.string.delete_card_confirm_msg))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                Log.i(TAG, "User confirmed card deletion")
                deleteCard()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                Log.d(TAG, "User cancelled card deletion")
            }
            .show()
    }

    /**
     * Calls [viewModel.deleteFlashcard] to delete the card identified by [remoteCardId] from [remoteDeckId].
     */
    private fun deleteCard() {
        val deckId = remoteDeckId
        val cardId = remoteCardId
        Log.d(TAG, "deleteCard: Attempting deletion for deckId=$deckId, cardId=$cardId")

        if (deckId != null && cardId != null) {
            viewModel.deleteFlashcard(deckId, cardId) { success ->
                if (success) {
                    Log.i(TAG, "Card $cardId deleted successfully from deck $deckId")
                    Toast.makeText(this, "Card deleted", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Log.e(TAG, "Failed to delete card $cardId from deck $deckId")
                    Toast.makeText(this, "Failed to delete card", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.w(TAG, "deleteCard called with missing parameters: deckId=$deckId, cardId=$cardId")
        }
    }
}
