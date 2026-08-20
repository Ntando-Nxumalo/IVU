package com.ntando.ivu.data.repository

import com.ntando.ivu.network.ApiClient
import com.ntando.ivu.network.JournalEntry
import com.ntando.ivu.network.CreateJournalRequest
import com.ntando.ivu.network.ApiResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class JournalRepository(
    private val achievementRepository: AchievementRepository? = null,
    private val userId: Long = -1
) {

    suspend fun fetchEntries(): Result<List<JournalEntry>> {
        return try {
            val response = ApiClient.apiService.getJournalEntries()
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

    suspend fun createEntry(date: String, mood: String, text: String, linkedDeckId: String?): Result<JournalEntry> {
        return try {
            val request = CreateJournalRequest(date, mood, text, linkedDeckId)
            val response = ApiClient.apiService.createJournalEntry(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    achievementRepository?.recordActivity(userId, ActivityType.JOURNAL_ENTRY)
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: "Failed to create entry"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
