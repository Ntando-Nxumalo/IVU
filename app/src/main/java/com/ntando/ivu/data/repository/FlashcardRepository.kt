package com.ntando.ivu.data.repository

import android.util.Log
import com.ntando.ivu.network.ApiClient
import com.ntando.ivu.network.Flashcard
import com.ntando.ivu.network.ReviewRequest
import com.ntando.ivu.network.CreateFlashcardRequest
import com.ntando.ivu.network.ApiResponse
import com.ntando.ivu.data.entity.Badge
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Repository for managing Flashcard data.
 * Handles fetching, creating, deleting cards and submitting reviews to the backend REST API.
 * Integrates with [AchievementRepository] to trigger gamification updates locally upon review submission.
 *
 * @property achievementRepository Optional [AchievementRepository] to award badges on review completion.
 * @property userId Current user identifier used for recording achievement progress.
 */
class FlashcardRepository(
    private val achievementRepository: AchievementRepository? = null,
    private val userId: String = ""
) {
    private val tag = "FlashcardRepository"

    /**
     * Fetches all flashcards belonging to the specified deck.
     *
     * @param deckId Identifier of the target deck.
     * @return [Result] wrapping a list of [Flashcard] objects on success, or an [Exception] on failure.
     */
    suspend fun fetchAllCards(deckId: String): Result<List<Flashcard>> {
        Log.d(tag, "fetchAllCards: Fetching cards for deck $deckId")
        return try {
            val response = ApiClient.apiService.getCards(deckId)
            Log.d(tag, "fetchAllCards: Received response code=${response.code()}")
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val cards = body.data ?: emptyList()
                    Log.i(tag, "fetchAllCards: Successfully fetched ${cards.size} cards for deck $deckId")
                    Result.success(cards)
                } else {
                    Log.w(tag, "fetchAllCards: API error: ${body?.error}")
                    Result.failure(Exception(body?.error ?: "Unknown error"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(tag, "fetchAllCards: Server error ${response.code()} - $errorBody")
                val parsedError = try {
                    val type = object : TypeToken<ApiResponse<Any>>() {}.type
                    val apiResponse: ApiResponse<Any> = Gson().fromJson(errorBody, type)
                    apiResponse.error ?: "Network error: ${response.code()}"
                } catch (e: Exception) {
                    Log.w(tag, "fetchAllCards: Failed to parse error response body", e)
                    "Network error: ${response.code()}"
                }
                Result.failure(Exception(parsedError))
            }
        } catch (e: Exception) {
            Log.e(tag, "fetchAllCards: Exception thrown while fetching cards for deck $deckId", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches due flashcards that are scheduled for review within the specified deck.
     *
     * @param deckId Identifier of the target deck.
     * @return [Result] wrapping a list of due [Flashcard] items on success, or an [Exception] on failure.
     */
    suspend fun fetchDueCards(deckId: String): Result<List<Flashcard>> {
        Log.d(tag, "fetchDueCards: Fetching due cards for deck $deckId")
        return try {
            val response = ApiClient.apiService.getDueCards(deckId)
            Log.d(tag, "fetchDueCards: Received response code=${response.code()}")
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val dueCards = body.data ?: emptyList()
                    Log.i(tag, "fetchDueCards: Successfully fetched ${dueCards.size} due cards for deck $deckId")
                    Result.success(dueCards)
                } else {
                    Log.w(tag, "fetchDueCards: API error response: ${body?.error}")
                    Result.failure(Exception(body?.error ?: "Unknown error"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(tag, "fetchDueCards: Server error ${response.code()} - $errorBody")
                val parsedError = try {
                    val type = object : TypeToken<ApiResponse<Any>>() {}.type
                    val apiResponse: ApiResponse<Any> = Gson().fromJson(errorBody, type)
                    apiResponse.error ?: "Network error: ${response.code()}"
                } catch (e: Exception) {
                    Log.w(tag, "fetchDueCards: Failed to parse error response body", e)
                    "Network error: ${response.code()}"
                }
                Result.failure(Exception(parsedError))
            }
        } catch (e: Exception) {
            Log.e(tag, "fetchDueCards: Exception thrown while fetching due cards for deck $deckId", e)
            Result.failure(e)
        }
    }

    /**
     * Submits a flashcard review rating (e.g. "again", "hard", "good", "easy") to the backend.
     * Updates gamification statistics and awards unlocked badges if an [AchievementRepository] is provided.
     *
     * @param deckId Identifier of the deck containing the card.
     * @param cardId Identifier of the card being reviewed.
     * @param rating Rating value string ("again", "hard", "good", "easy").
     * @return [Result] containing a [Pair] of the updated [Flashcard] and a list of newly unlocked [Badge] items.
     */
    suspend fun submitReview(deckId: String, cardId: String, rating: String): Result<Pair<Flashcard, List<Badge>>> {
        Log.d(tag, "submitReview: Submitting review for cardId=$cardId in deckId=$deckId with rating='$rating'")
        return try {
            val request = ReviewRequest(rating)
            val response = ApiClient.apiService.reviewCard(deckId, cardId, request)
            Log.d(tag, "submitReview: Received response code=${response.code()}")
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Log.i(tag, "submitReview: Successfully reviewed cardId=$cardId")
                    val unlockedBadges = achievementRepository?.recordReview(userId) ?: emptyList()
                    if (unlockedBadges.isNotEmpty()) {
                        Log.i(tag, "submitReview: Unlocked ${unlockedBadges.size} badges following review")
                    }
                    Result.success(Pair(body.data, unlockedBadges))
                } else {
                    val errorMsg = body?.error ?: "Failed to submit review"
                    Log.w(tag, "submitReview: Failed to submit review - $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } else {
                Log.e(tag, "submitReview: Server error HTTP ${response.code()} for cardId=$cardId")
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "submitReview: Exception thrown while submitting review for cardId=$cardId", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a new flashcard in the specified deck.
     *
     * @param deckId Identifier of the deck.
     * @param front Text/prompt for the front of the flashcard.
     * @param back Text/answer for the back of the flashcard.
     * @return [Result] containing the created [Flashcard] on success, or an [Exception] on failure.
     */
    suspend fun createCard(deckId: String, front: String, back: String): Result<Flashcard> {
        Log.d(tag, "createCard: Creating card in deckId=$deckId (frontLength=${front.length}, backLength=${back.length})")
        return try {
            val request = CreateFlashcardRequest(front, back)
            val response = ApiClient.apiService.createCard(deckId, request)
            Log.d(tag, "createCard: Received response code=${response.code()}")
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Log.i(tag, "createCard: Successfully created cardId=${body.data.cardId} in deckId=$deckId")
                    Result.success(body.data)
                } else {
                    val errorMsg = body?.error ?: "Failed to create card"
                    Log.w(tag, "createCard: API error creating card - $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } else {
                Log.e(tag, "createCard: Server error HTTP ${response.code()} for deckId=$deckId")
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "createCard: Exception thrown while creating card in deck $deckId", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a flashcard from a deck by its card ID.
     *
     * @param deckId Identifier of the deck containing the card.
     * @param cardId Identifier of the card to delete.
     * @return [Result] containing [Unit] on success, or an [Exception] on failure.
     */
    suspend fun deleteCard(deckId: String, cardId: String): Result<Unit> {
        Log.d(tag, "deleteCard: Attempting to delete cardId=$cardId from deckId=$deckId")
        return try {
            val response = ApiClient.apiService.deleteCard(deckId, cardId)
            Log.d(tag, "deleteCard: Received response code=${response.code()}")
            if (response.isSuccessful && response.body()?.success == true) {
                Log.i(tag, "deleteCard: Successfully deleted cardId=$cardId from deckId=$deckId")
                Result.success(Unit)
            } else {
                Log.w(tag, "deleteCard: Failed to delete cardId=$cardId from deckId=$deckId")
                Result.failure(Exception("Failed to delete card"))
            }
        } catch (e: Exception) {
            Log.e(tag, "deleteCard: Exception thrown while deleting cardId=$cardId", e)
            Result.failure(e)
        }
    }
}
