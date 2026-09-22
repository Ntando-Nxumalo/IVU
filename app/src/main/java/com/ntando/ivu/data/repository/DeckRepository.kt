package com.ntando.ivu.data.repository

import android.util.Log
import com.ntando.ivu.network.ApiClient
import com.ntando.ivu.network.CreateDeckRequest
import com.ntando.ivu.network.Deck
import com.ntando.ivu.network.ApiResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Repository responsible for managing flashcard decks.
 * Handles fetching existing decks, creating new decks, and deleting decks via [ApiClient.apiService].
 */
class DeckRepository {
    private val TAG = "DeckRepository"

    /**
     * Fetches all flashcard decks associated with the authenticated user.
     *
     * @return [Result] containing a list of [Deck] objects on success, or an [Exception] on error.
     */
    suspend fun fetchDecks(): Result<List<Deck>> {
        Log.d(TAG, "fetchDecks: Initiating request to fetch user decks")
        return try {
            val response = ApiClient.apiService.getDecks()
            Log.d(TAG, "fetchDecks: Received response code=${response.code()}")
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val decks = body.data ?: emptyList()
                    Log.i(TAG, "fetchDecks: Successfully fetched ${decks.size} decks")
                    Result.success(decks)
                } else {
                    val errorMessage = body?.error ?: "Unknown error"
                    Log.w(TAG, "fetchDecks: API returned failure response error='$errorMessage'")
                    Result.failure(Exception(errorMessage))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "fetchDecks: HTTP ${response.code()} error - $errorBody")
                val parsedError = try {
                    val type = object : TypeToken<ApiResponse<Any>>() {}.type
                    val apiResponse: ApiResponse<Any> = Gson().fromJson(errorBody, type)
                    apiResponse.error ?: "Network error: ${response.code()}"
                } catch (e: Exception) {
                    Log.w(TAG, "fetchDecks: Failed to parse error response body", e)
                    "Network error: ${response.code()}"
                }
                Result.failure(Exception(parsedError))
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchDecks: Exception caught while fetching decks", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a new flashcard deck with the specified title and target language.
     *
     * @param title Title of the deck.
     * @param language Language code for the deck (e.g., "en", "zu", "af").
     * @return [Result] wrapping the newly created [Deck] on success, or an [Exception] on error.
     */
    suspend fun createDeck(title: String, language: String): Result<Deck> {
        Log.d(TAG, "createDeck: Requesting deck creation with title='$title', language='$language'")
        return try {
            val request = CreateDeckRequest(title, language)
            val response = ApiClient.apiService.createDeck(request)
            Log.d(TAG, "createDeck: Received response code=${response.code()}")
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Log.i(TAG, "createDeck: Successfully created deck with ID=${body.data.deckId}")
                    Result.success(body.data)
                } else {
                    val errorMessage = body?.error ?: "Failed to create deck"
                    Log.w(TAG, "createDeck: API returned failure response error='$errorMessage'")
                    Result.failure(Exception(errorMessage))
                }
            } else {
                Log.e(TAG, "createDeck: HTTP ${response.code()} error during deck creation")
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "createDeck: Exception caught while creating deck '$title'", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a flashcard deck by its unique identifier.
     *
     * @param deckId Unique identifier of the deck to delete.
     * @return [Result] containing [Unit] on success, or an [Exception] on error.
     */
    suspend fun deleteDeck(deckId: String): Result<Unit> {
        Log.d(TAG, "deleteDeck: Attempting to delete deckId=$deckId")
        return try {
            val response = ApiClient.apiService.deleteDeck(deckId)
            Log.d(TAG, "deleteDeck: Received response code=${response.code()}")
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Log.i(TAG, "deleteDeck: Deck $deckId deleted successfully")
                    Result.success(Unit)
                } else {
                    val errorMessage = body?.error ?: "Failed to delete deck"
                    Log.w(TAG, "deleteDeck: API returned failure error='$errorMessage'")
                    Result.failure(Exception(errorMessage))
                }
            } else {
                Log.e(TAG, "deleteDeck: HTTP ${response.code()} error deleting deck $deckId")
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteDeck: Exception caught while deleting deckId=$deckId", e)
            Result.failure(e)
        }
    }
}
