package com.ntando.ivu.network

import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit service interface defining the REST API endpoints for the IVU application.
 * Manages HTTP communication for Decks, Flashcards, Journal entries, and AI interactions.
 */
interface IvuApiService {

    // --------- Decks ---------

    /**
     * Fetches all flashcard decks associated with the authenticated user.
     * Endpoint: `GET decks`
     *
     * @return [Response] containing [ApiResponse] with a list of [Deck] objects.
     */
    @GET("decks")
    suspend fun getDecks(): Response<ApiResponse<List<Deck>>>

    /**
     * Creates a new flashcard deck.
     * Endpoint: `POST decks`
     *
     * @param request [CreateDeckRequest] object containing title and language.
     * @return [Response] containing [ApiResponse] wrapping the created [Deck].
     */
    @POST("decks")
    suspend fun createDeck(@Body request: CreateDeckRequest): Response<ApiResponse<Deck>>

    /**
     * Deletes a flashcard deck by ID.
     * Endpoint: `DELETE decks/{deckId}`
     *
     * @param deckId Unique identifier of the deck to delete.
     * @return [Response] containing [ApiResponse] with [DeleteResponse] confirmation.
     */
    @DELETE("decks/{deckId}")
    suspend fun deleteDeck(@Path("deckId") deckId: String): Response<ApiResponse<DeleteResponse>>

    // --------- Flashcards ---------

    /**
     * Retrieves all flashcards contained within a specific deck.
     * Endpoint: `GET decks/{deckId}/cards`
     *
     * @param deckId Unique identifier of the target deck.
     * @return [Response] containing [ApiResponse] with a list of [Flashcard] objects.
     */
    @GET("decks/{deckId}/cards")
    suspend fun getCards(@Path("deckId") deckId: String): Response<ApiResponse<List<Flashcard>>>

    /**
     * Retrieves flashcards due for review within a specific deck based on SRS scheduling.
     * Endpoint: `GET decks/{deckId}/cards/due`
     *
     * @param deckId Unique identifier of the target deck.
     * @return [Response] containing [ApiResponse] with a list of due [Flashcard] objects.
     */
    @GET("decks/{deckId}/cards/due")
    suspend fun getDueCards(@Path("deckId") deckId: String): Response<ApiResponse<List<Flashcard>>>

    /**
     * Creates a new flashcard inside the specified deck.
     * Endpoint: `POST decks/{deckId}/cards`
     *
     * @param deckId Target deck identifier.
     * @param request [CreateFlashcardRequest] containing frontText, backText, and optional imageUrl.
     * @return [Response] containing [ApiResponse] wrapping the created [Flashcard].
     */
    @POST("decks/{deckId}/cards")
    suspend fun createCard(
        @Path("deckId") deckId: String,
        @Body request: CreateFlashcardRequest
    ): Response<ApiResponse<Flashcard>>

    /**
     * Submits a spaced repetition review rating for a flashcard.
     * Endpoint: `PUT decks/{deckId}/cards/{cardId}/review`
     *
     * @param deckId Parent deck identifier.
     * @param cardId Target flashcard identifier.
     * @param request [ReviewRequest] containing rating ("again", "hard", "good", "easy").
     * @return [Response] containing [ApiResponse] wrapping the updated [Flashcard].
     */
    @PUT("decks/{deckId}/cards/{cardId}/review")
    suspend fun reviewCard(
        @Path("deckId") deckId: String,
        @Path("cardId") cardId: String,
        @Body request: ReviewRequest
    ): Response<ApiResponse<Flashcard>>

    /**
     * Deletes an individual flashcard from a deck.
     * Endpoint: `DELETE decks/{deckId}/cards/{cardId}`
     *
     * @param deckId Parent deck identifier.
     * @param cardId Target flashcard identifier.
     * @return [Response] containing [ApiResponse] with [DeleteResponse] status.
     */
    @DELETE("decks/{deckId}/cards/{cardId}")
    suspend fun deleteCard(
        @Path("deckId") deckId: String,
        @Path("cardId") cardId: String
    ): Response<ApiResponse<DeleteResponse>>

    // --------- Journal ---------

    /**
     * Fetches all journal entries for the current user.
     * Endpoint: `GET journal`
     *
     * @return [Response] containing [ApiResponse] with a list of [JournalEntry] objects.
     */
    @GET("journal")
    suspend fun getJournalEntries(): Response<ApiResponse<List<JournalEntry>>>

    /**
     * Creates a new journal entry.
     * Endpoint: `POST journal`
     *
     * @param request [CreateJournalRequest] containing date, mood, text, and optional linkedDeckId.
     * @return [Response] containing [ApiResponse] wrapping the newly created [JournalEntry].
     */
    @POST("journal")
    suspend fun createJournalEntry(@Body request: CreateJournalRequest): Response<ApiResponse<JournalEntry>>

    // --------- AI ---------

    /**
     * Sends a user prompt to the backend AI assistant service.
     * Endpoint: `POST ai/ask`
     *
     * @param request [AiRequest] containing the prompt text.
     * @return [Response] containing [ApiResponse] wrapping [AiReplyResponse].
     */
    @POST("ai/ask")
    suspend fun askAI(@Body request: AiRequest): Response<ApiResponse<AiReplyResponse>>
}
