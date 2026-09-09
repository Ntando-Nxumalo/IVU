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
 * Handles fetching, creating, deleting cards and submitting reviews to the Render REST API.
 * Integrates with AchievementRepository to trigger gamification updates locally.
 */
class FlashcardRepository(
    private val achievementRepository: AchievementRepository? = null,
    private val userId: String = ""
) {
    private val tag = "FlashcardRepository"

    suspend fun fetchAllCards(deckId: String): Result<List<Flashcard>> {
        Log.d(tag, "fetchAllCards: Fetching cards for deck $deckId")
        return try {
            val response = ApiClient.apiService.getCards(deckId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Log.i(tag, "fetchAllCards: Successfully fetched ${body.data?.size} cards")
                    Result.success(body.data ?: emptyList())
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
                    "Network error: ${response.code()}"
                }
                Result.failure(Exception(parsedError))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchDueCards(deckId: String): Result<List<Flashcard>> {
        return try {
            val response = ApiClient.apiService.getDueCards(deckId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Result.success(body.data ?: emptyList())
                } else {
                    Result.failure(Exception(body?.error ?: "Unknown error"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val parsedError = try {
                    val type = object : TypeToken<ApiResponse<Any>>() {}.type
                    val apiResponse: ApiResponse<Any> = Gson().fromJson(errorBody, type)
                    apiResponse.error ?: "Network error: ${response.code()}"
                } catch (e: Exception) {
                    "Network error: ${response.code()}"
                }
                Result.failure(Exception(parsedError))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitReview(deckId: String, cardId: String, rating: String): Result<Pair<Flashcard, List<Badge>>> {
        return try {
            val request = ReviewRequest(rating)
            val response = ApiClient.apiService.reviewCard(deckId, cardId, request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    val unlockedBadges = achievementRepository?.recordReview(userId) ?: emptyList()
                    Result.success(Pair(body.data, unlockedBadges))
                } else {
                    Result.failure(Exception(body?.error ?: "Failed to submit review"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCard(deckId: String, front: String, back: String): Result<Flashcard> {
        return try {
            val request = CreateFlashcardRequest(front, back)
            val response = ApiClient.apiService.createCard(deckId, request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: "Failed to create card"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteCard(deckId: String, cardId: String): Result<Unit> {
        return try {
            val response = ApiClient.apiService.deleteCard(deckId, cardId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete card"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
