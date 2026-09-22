package com.ntando.ivu.network

/**
 * Generic API wrapper response object returned by the server.
 *
 * @param T The type of data contained in the response payload.
 * @property success Indicates whether the network operation succeeded.
 * @property data The response payload object of type [T], or `null` if the operation failed.
 * @property error Error description string if [success] is `false`, otherwise `null`.
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: String?
)

/**
 * Represents a flashcard deck entity retrieved from the backend API.
 *
 * @property deckId Server-assigned unique identifier for the deck.
 * @property userId Unique identifier of the user who owns this deck.
 * @property title Title/name of the deck.
 * @property language Target language code for the deck (e.g. "en", "zu", "af").
 * @property cardCount Total number of cards contained within this deck. Defaults to 0.
 * @property createdAt Timestamp (in milliseconds) when the deck was created.
 */
data class Deck(
    val deckId: String? = null,
    val userId: String? = null,
    val title: String,
    val language: String, // "en" | "zu" | "af"
    val cardCount: Int = 0,
    val createdAt: Long? = null
)

/**
 * Request payload for creating a new flashcard deck.
 *
 * @property title Desired title for the new deck.
 * @property language Target language code (e.g., "en", "zu", "af").
 */
data class CreateDeckRequest(
    val title: String,
    val language: String
)

/**
 * Represents an individual flashcard entity.
 *
 * @property cardId Server-assigned unique identifier for the flashcard.
 * @property frontText Prompt or term displayed on the front of the flashcard.
 * @property backText Answer or translation displayed on the back of the flashcard.
 * @property imageUrl Optional URL of an image associated with this flashcard.
 * @property easeFactor Spaced repetition ease factor (SuperMemo SM-2 algorithm). Defaults to 2.5.
 * @property intervalDays Scheduled interval in days before the next review. Defaults to 0.
 * @property repetitions Number of consecutive correct reviews completed. Defaults to 0.
 * @property dueDate Unix timestamp (in milliseconds) indicating when this card is due for review.
 */
data class Flashcard(
    val cardId: String? = null,
    val frontText: String,
    val backText: String,
    val imageUrl: String? = null,
    val easeFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val dueDate: Long = 0L
)

/**
 * Request payload for creating a new flashcard.
 *
 * @property frontText Prompt or term text for the front of the flashcard.
 * @property backText Definition or translation text for the back of the flashcard.
 * @property imageUrl Optional image URL attachment for the card.
 */
data class CreateFlashcardRequest(
    val frontText: String,
    val backText: String,
    val imageUrl: String? = null
)

/**
 * Request payload when submitting a review rating for a flashcard.
 *
 * @property rating Review rating string. Expected values: "again", "hard", "good", "easy".
 */
data class ReviewRequest(
    val rating: String // "again" | "hard" | "good" | "easy"
)

/**
 * Represents a journal entry recorded by the user.
 *
 * @property entryId Unique identifier for the journal entry.
 * @property userId Unique identifier of the user who created the journal entry.
 * @property date Formatted date string representing when the entry was written.
 * @property mood Self-reported mood rating (e.g., "great", "okay", "tough").
 * @property text Content text of the journal entry.
 * @property linkedDeckId Optional deck identifier linked to this journal entry.
 * @property createdAt Server creation timestamp in milliseconds.
 */
data class JournalEntry(
    val entryId: String? = null,
    val userId: String? = null,
    val date: String,
    val mood: String, // "great" | "okay" | "tough"
    val text: String,
    val linkedDeckId: String? = null,
    val createdAt: Long? = null
)

/**
 * Request payload for creating a new journal entry.
 *
 * @property date Formatted date string for the journal record.
 * @property mood User's mood state ("great", "okay", "tough").
 * @property text Text content of the journal.
 * @property linkedDeckId Optional deck ID to associate with this journal entry.
 */
data class CreateJournalRequest(
    val date: String,
    val mood: String,
    val text: String,
    val linkedDeckId: String? = null
)

/**
 * Response object returned following deletion requests for decks, cards, or journal entries.
 *
 * @property deckId Optional ID of deleted deck.
 * @property cardId Optional ID of deleted flashcard.
 * @property entryId Optional ID of deleted journal entry.
 * @property deleted Indicates whether the deletion succeeded on the server.
 */
data class DeleteResponse(
    val deckId: String? = null,
    val cardId: String? = null,
    val entryId: String? = null,
    val deleted: Boolean
)

/**
 * Request payload for AI query prompts.
 *
 * @property prompt User prompt text sent to the AI backend.
 */
data class AiRequest(
    val prompt: String
)

/**
 * Response payload containing AI model generated text reply.
 *
 * @property reply Generated AI response text.
 */
data class AiReplyResponse(
    val reply: String
)
