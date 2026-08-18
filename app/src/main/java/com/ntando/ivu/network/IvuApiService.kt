package com.ntando.ivu.network

import retrofit2.Response
import retrofit2.http.*

interface IvuApiService {

    // --------- Decks ---------

    @GET("decks")
    suspend fun getDecks(): Response<ApiResponse<List<Deck>>>

    @POST("decks")
    suspend fun createDeck(@Body request: CreateDeckRequest): Response<ApiResponse<Deck>>

    @DELETE("decks/{deckId}")
    suspend fun deleteDeck(@Path("deckId") deckId: String): Response<ApiResponse<DeleteResponse>>

    // --------- Flashcards ---------

    @GET("decks/{deckId}/cards")
    suspend fun getCards(@Path("deckId") deckId: String): Response<ApiResponse<List<Flashcard>>>

    @GET("decks/{deckId}/cards/due")
    suspend fun getDueCards(@Path("deckId") deckId: String): Response<ApiResponse<List<Flashcard>>>

    @POST("decks/{deckId}/cards")
    suspend fun createCard(
        @Path("deckId") deckId: String,
        @Body request: CreateFlashcardRequest
    ): Response<ApiResponse<Flashcard>>

    @PUT("decks/{deckId}/cards/{cardId}/review")
    suspend fun reviewCard(
        @Path("deckId") deckId: String,
        @Path("cardId") cardId: String,
        @Body request: ReviewRequest
    ): Response<ApiResponse<Flashcard>>

    @DELETE("decks/{deckId}/cards/{cardId}")
    suspend fun deleteCard(
        @Path("deckId") deckId: String,
        @Path("cardId") cardId: String
    ): Response<ApiResponse<DeleteResponse>>

    // --------- Journal ---------

    @GET("journal")
    suspend fun getJournalEntries(): Response<ApiResponse<List<JournalEntry>>>

    @POST("journal")
    suspend fun createJournalEntry(@Body request: CreateJournalRequest): Response<ApiResponse<JournalEntry>>
}
