package com.ntando.ivu.network

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor that automatically retrieves the current Firebase user's ID token
 * and attaches it as a "Bearer <token>" Authorization HTTP header on every outgoing request.
 *
 * Forces token refresh (`getIdToken(true)`) to guarantee the token is valid prior to API transmission.
 */
class AuthInterceptor : Interceptor {
    private val TAG = "AuthInterceptor"

    /**
     * Intercepts outgoing HTTP requests, fetches a fresh Firebase ID token if a user is logged in,
     * appends the Authorization header, executes the request, and logs the outcome.
     *
     * @param chain OkHttp interceptor chain.
     * @return [Response] resulting from the request execution.
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val user = FirebaseAuth.getInstance().currentUser

        val token = if (user != null) {
            try {
                // Force refresh the token to ensure it's valid
                runBlocking {
                    val result = user.getIdToken(true).await()
                    Log.d(TAG, "Successfully fetched fresh token for user: ${user.email}")
                    result.token
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch Firebase ID token for user: ${user.email}", e)
                null
            }
        } else {
            Log.w(TAG, "No active Firebase user found - sending request without Authorization token")
            null
        }

        val newRequest = if (token != null) {
            Log.d(TAG, "Attaching Bearer token to request URL: ${originalRequest.url}")
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(newRequest)
        
        if (!response.isSuccessful) {
            Log.e(TAG, "HTTP Request failed with status code: ${response.code} for URL: ${originalRequest.url}")
        } else {
            Log.d(TAG, "HTTP Request succeeded with status code: ${response.code} for URL: ${originalRequest.url}")
        }
        
        return response
    }
}
