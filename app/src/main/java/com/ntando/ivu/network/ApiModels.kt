package com.ntando.ivu.network

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: String?
)

data class Deck(
    val deckId: String? = null,
    val userId: String? = null,
    val title: String,
    val language: String, // "en" | "zu" | "af"
    val cardCount: Int = 0,
    val createdAt: Long? = null
)

data class CreateDeckRequest(
    val title: String,
    val language: String
)

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

data class CreateFlashcardRequest(
    val frontText: String,
    val backText: String,
    val imageUrl: String? = null
)

data class ReviewRequest(
    val rating: String // "again" | "hard" | "good" | "easy"
)

data class JournalEntry(
    val entryId: String? = null,
    val userId: String? = null,
    val date: String,
    val mood: String, // "great" | "okay" | "tough"
    val text: String,
    val linkedDeckId: String? = null,
    val createdAt: Long? = null
)

data class CreateJournalRequest(
    val date: String,
    val mood: String,
    val text: String,
    val linkedDeckId: String? = null
)

data class DeleteResponse(
    val deckId: String? = null,
    val cardId: String? = null,
    val entryId: String? = null,
    val deleted: Boolean
)

data class AiRequest(
    val prompt: String
)

data class AiReplyResponse(
    val reply: String
)
