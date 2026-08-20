package com.ntando.ivu.network

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Automatically attaches the current Firebase user's ID token as a
 * "Bearer <token>" Authorization header on every outgoing request.
 */
class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val user = FirebaseAuth.getInstance().currentUser

        val token = if (user != null) {
            try {
                // Force refresh the token to ensure it's valid
                runBlocking {
                    val result = user.getIdToken(true).await()
                    Log.d("AuthInterceptor", "Successfully fetched token for: ${user.email}")
                    result.token
                }
            } catch (e: Exception) {
                Log.e("AuthInterceptor", "Failed to fetch Firebase token", e)
                null
            }
        } else {
            Log.w("AuthInterceptor", "No Firebase user found - sending request without token")
            null
        }

        val newRequest = if (token != null) {
            Log.d("AuthInterceptor", "Attaching Bearer token to: ${originalRequest.url}")
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(newRequest)
        
        if (!response.isSuccessful) {
            Log.e("AuthInterceptor", "Request failed with code: ${response.code} for URL: ${originalRequest.url}")
        }
        
        return response
    }
}
