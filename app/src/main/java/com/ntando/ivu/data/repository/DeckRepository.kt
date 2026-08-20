package com.ntando.ivu.data.repository

import com.ntando.ivu.network.ApiClient
import com.ntando.ivu.network.CreateDeckRequest
import com.ntando.ivu.network.Deck
import com.ntando.ivu.network.ApiResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DeckRepository {

    suspend fun fetchDecks(): Result<List<Deck>> {
        return try {
            val response = ApiClient.apiService.getDecks()
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

    suspend fun createDeck(title: String, language: String): Result<Deck> {
        return try {
            val request = CreateDeckRequest(title, language)
            val response = ApiClient.apiService.createDeck(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: "Failed to create deck"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDeck(deckId: String): Result<Unit> {
        return try {
            val response = ApiClient.apiService.deleteDeck(deckId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(body?.error ?: "Failed to delete deck"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
