package com.ntando.ivu.data.repository

import android.util.Log
import com.ntando.ivu.network.AiRequest
import com.ntando.ivu.network.ApiClient

/**
 * Repository for interacting with the AI chat services via network API.
 * Delegates AI prompt interactions to [ApiClient.apiService].
 */
class ChatRepository {
    private val TAG = "ChatRepository"

    /**
     * Sends a user chat prompt to the AI backend and retrieves the response reply.
     *
     * @param prompt User's prompt text to send to the AI service.
     * @return [Result] wrapping the AI response string on success, or an [Exception] on error.
     */
    suspend fun sendMessage(prompt: String): Result<String> {
        Log.d(TAG, "sendMessage: Sending prompt to AI (length=${prompt.length})")
        return try {
            val response = ApiClient.apiService.askAI(AiRequest(prompt))
            Log.d(TAG, "sendMessage: Received HTTP response code=${response.code()}")
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Log.i(TAG, "sendMessage: AI reply received successfully (length=${body.data.reply.length})")
                    Result.success(body.data.reply)
                } else {
                    val errorMessage = body?.error ?: "Failed to get AI response"
                    Log.w(TAG, "sendMessage: Backend returned failure state - error='$errorMessage'")
                    Result.failure(Exception(errorMessage))
                }
            } else {
                val fullUrl = response.raw().request.url
                Log.e(TAG, "sendMessage: HTTP ${response.code()} error at $fullUrl - Check if endpoint exists on backend.")
                Result.failure(Exception("Network error: ${response.code()} (Endpoint not found)"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage: Exception caught during AI API call", e)
            Result.failure(e)
        }
    }
}
