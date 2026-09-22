package com.ntando.ivu.data.repository

import android.util.Log
import com.ntando.ivu.network.ApiClient
import com.ntando.ivu.network.JournalEntry
import com.ntando.ivu.network.CreateJournalRequest
import com.ntando.ivu.network.ApiResponse
import com.ntando.ivu.data.entity.Badge
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Repository responsible for managing user journal entries.
 * Facilitates fetching journal records and creating new journal entries via the network API.
 * Integrates with [AchievementRepository] to record journal creation activities and award gamification badges.
 *
 * @property achievementRepository Optional [AchievementRepository] to record journal entries for user achievements.
 * @property userId Unique identifier for the current user.
 */
class JournalRepository(
    private val achievementRepository: AchievementRepository? = null,
    private val userId: String = ""
) {
    private val TAG = "JournalRepository"

    /**
     * Fetches all journal entries for the current authenticated user from the backend server.
     *
     * @return [Result] wrapping a list of [JournalEntry] objects on success, or an [Exception] on error.
     */
    suspend fun fetchEntries(): Result<List<JournalEntry>> {
        Log.d(TAG, "fetchEntries: Requesting user journal entries")
        return try {
            val response = ApiClient.apiService.getJournalEntries()
            Log.d(TAG, "fetchEntries: Received response code=${response.code()}")
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val entries = body.data ?: emptyList()
                    Log.i(TAG, "fetchEntries: Successfully fetched ${entries.size} journal entries")
                    Result.success(entries)
                } else {
                    val errorMessage = body?.error ?: "Unknown error"
                    Log.w(TAG, "fetchEntries: API error response - $errorMessage")
                    Result.failure(Exception(errorMessage))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "fetchEntries: HTTP ${response.code()} error - $errorBody")
                val parsedError = try {
                    val type = object : TypeToken<ApiResponse<Any>>() {}.type
                    val apiResponse: ApiResponse<Any> = Gson().fromJson(errorBody, type)
                    apiResponse.error ?: "Network error: ${response.code()}"
                } catch (e: Exception) {
                    Log.w(TAG, "fetchEntries: Failed to parse error response body", e)
                    "Network error: ${response.code()}"
                }
                Result.failure(Exception(parsedError))
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchEntries: Exception caught while fetching journal entries", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a new journal entry with mood tracking and optional deck linking.
     * Triggers badge progress tracking upon successful creation.
     *
     * @param date Date string for the entry.
     * @param mood Recorded mood string ("great", "okay", "tough").
     * @param text Journal content text.
     * @param linkedDeckId Optional identifier of a deck linked to this journal entry.
     * @return [Result] containing a [Pair] of the created [JournalEntry] and a list of newly unlocked [Badge] instances.
     */
    suspend fun createEntry(date: String, mood: String, text: String, linkedDeckId: String?): Result<Pair<JournalEntry, List<Badge>>> {
        Log.d(TAG, "createEntry: Creating journal entry for date='$date', mood='$mood', linkedDeckId=$linkedDeckId")
        return try {
            val request = CreateJournalRequest(date, mood, text, linkedDeckId)
            val response = ApiClient.apiService.createJournalEntry(request)
            Log.d(TAG, "createEntry: Received response code=${response.code()}")
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Log.i(TAG, "createEntry: Journal entry created successfully with entryId=${body.data.entryId}")
                    val unlockedBadges = achievementRepository?.recordJournalEntry(userId) ?: emptyList()
                    if (unlockedBadges.isNotEmpty()) {
                        Log.i(TAG, "createEntry: Unlocked ${unlockedBadges.size} badges after journal creation")
                    }
                    Result.success(Pair(body.data, unlockedBadges))
                } else {
                    val errorMsg = body?.error ?: "Failed to create entry"
                    Log.w(TAG, "createEntry: API error creating entry - $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } else {
                Log.e(TAG, "createEntry: Server error HTTP ${response.code()}")
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "createEntry: Exception caught while creating journal entry", e)
            Result.failure(e)
        }
    }
}
