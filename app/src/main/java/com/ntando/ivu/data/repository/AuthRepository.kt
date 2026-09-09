package com.ntando.ivu.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

/**
 * Repository handling Firebase Authentication flows.
 * Manages user registration, login, and profile updates.
 */
class AuthRepository {
    private val tag = "AuthRepository"
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun registerUser(displayName: String, email: String, password: String): Result<String> {
        Log.d(tag, "registerUser: Attempting registration for $email")
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Log.i(tag, "registerUser: Firebase user created successfully")
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(profileUpdates).await()
                Result.success(user.uid)
            } else {
                Log.e(tag, "registerUser: User object is null after creation")
                Result.failure(Exception("User creation failed"))
            }
        } catch (e: Exception) {
            Log.e(tag, "registerUser: Exception during registration", e)
            Result.failure(Exception(mapError(e)))
        }
    }

    suspend fun loginUser(email: String, password: String): Result<String> {
        Log.d(tag, "loginUser: Attempting login for $email")
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Log.i(tag, "loginUser: Login successful for ${user.uid}")
                Result.success(user.uid)
            } else {
                Log.e(tag, "loginUser: User object is null after login")
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Log.e(tag, "loginUser: Exception during login", e)
            Result.failure(Exception(mapError(e)))
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun signOut() {
        auth.signOut()
    }

    private fun mapError(e: Exception): String {
        return when (e) {
            is FirebaseAuthException -> {
                when (e.errorCode) {
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Email already in use"
                    "ERROR_WEAK_PASSWORD" -> "Password is too weak"
                    "ERROR_INVALID_CREDENTIALS" -> "Invalid email or password"
                    "ERROR_USER_NOT_FOUND" -> "User not found"
                    "ERROR_WRONG_PASSWORD" -> "Wrong password"
                    else -> e.message ?: "Authentication error"
                }
            }
            else -> e.message ?: "Network error. Please try again."
        }
    }
}
