package com.ntando.ivu.network

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Automatically attaches the current Firebase user's ID token as a
 * "Bearer <token>" Authorization header on every outgoing request.
 *
 * If no user is logged in, the request is sent without a token
 * (the backend will reject it with 401, which is expected/correct).
 */
class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val token = try {
            runBlocking {
                FirebaseAuth.getInstance().currentUser
                    ?.getIdToken(false) // false = don't force-refresh unless expired
                    ?.await()
                    ?.token
            }
        } catch (e: Exception) {
            null
        }

        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}
