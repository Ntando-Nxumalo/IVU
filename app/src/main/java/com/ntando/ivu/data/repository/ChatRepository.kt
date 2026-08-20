package com.ntando.ivu.data.repository

import android.util.Log
import com.ntando.ivu.network.AiRequest
import com.ntando.ivu.network.ApiClient

class ChatRepository {

    suspend fun sendMessage(prompt: String): Result<String> {
        return try {
            val response = ApiClient.apiService.askAI(AiRequest(prompt))
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data.reply)
                } else {
                    Result.failure(Exception(body?.error ?: "Failed to get AI response"))
                }
            } else {
                val fullUrl = response.raw().request.url
                Log.e("ChatRepository", "404 Error at $fullUrl - Check if this endpoint exists on your backend.")
                Result.failure(Exception("Network error: ${response.code()} (Endpoint not found)"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Exception during AI call", e)
            Result.failure(e)
        }
    }
}
