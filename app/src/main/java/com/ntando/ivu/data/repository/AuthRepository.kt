package com.ntando.ivu.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

/**
 * Repository handling Firebase Authentication flows for the IVU application.
 * Manages user registration, authentication credential validation, session retrieval,
 * sign-out operations, and user profile updates.
 */
class AuthRepository {
    private val tag = "AuthRepository"
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Registers a new user with email and password via Firebase Auth, and sets their display name.
     *
     * @param displayName The name to assign to the user profile.
     * @param email User's email address.
     * @param password User's selected password.
     * @return [Result] wrapping the Firebase user ID string on success, or an [Exception] with user-friendly error message on failure.
     */
    suspend fun registerUser(displayName: String, email: String, password: String): Result<String> {
        Log.d(tag, "registerUser: Attempting registration for email='$email', displayName='$displayName'")
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Log.i(tag, "registerUser: Firebase user created successfully with UID=${user.uid}")
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(profileUpdates).await()
                Log.i(tag, "registerUser: Updated user profile display name successfully for UID=${user.uid}")
                Result.success(user.uid)
            } else {
                Log.e(tag, "registerUser: User object is null after user creation call")
                Result.failure(Exception("User creation failed"))
            }
        } catch (e: Exception) {
            val mappedErrorMessage = mapError(e)
            Log.e(tag, "registerUser: Exception during registration for email='$email': $mappedErrorMessage", e)
            Result.failure(Exception(mappedErrorMessage))
        }
    }

    /**
     * Authenticates an existing user using email and password.
     *
     * @param email Registered email address.
     * @param password Account password.
     * @return [Result] containing the user UID on successful login, or an [Exception] on error.
     */
    suspend fun loginUser(email: String, password: String): Result<String> {
        Log.d(tag, "loginUser: Attempting login for email='$email'")
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Log.i(tag, "loginUser: Login successful for user UID=${user.uid}")
                Result.success(user.uid)
            } else {
                Log.e(tag, "loginUser: FirebaseUser object is null after successful authentication call")
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            val mappedErrorMessage = mapError(e)
            Log.e(tag, "loginUser: Exception during login for email='$email': $mappedErrorMessage", e)
            Result.failure(Exception(mappedErrorMessage))
        }
    }

    /**
     * Retrieves the currently active Firebase authenticated user.
     *
     * @return The currently signed in [FirebaseUser], or `null` if no active session exists.
     */
    fun getCurrentUser(): FirebaseUser? {
        val user = auth.currentUser
        if (user != null) {
            Log.d(tag, "getCurrentUser: Active session found for UID=${user.uid}, email=${user.email}")
        } else {
            Log.d(tag, "getCurrentUser: No active authenticated user session found")
        }
        return user
    }

    /**
     * Signs out the currently authenticated user from Firebase Auth.
     */
    fun signOut() {
        val currentUid = auth.currentUser?.uid
        Log.i(tag, "signOut: Signing out current user (UID=$currentUid)")
        auth.signOut()
        Log.d(tag, "signOut: User successfully signed out")
    }

    /**
     * Maps raw exceptions (including [FirebaseAuthException]) to user-friendly error messages.
     *
     * @param e The caught [Exception].
     * @return Formatted error description suitable for UI presentation.
     */
    private fun mapError(e: Exception): String {
        val errorMsg = when (e) {
            is FirebaseAuthException -> {
                Log.w(tag, "mapError: FirebaseAuthException caught with errorCode='${e.errorCode}'", e)
                when (e.errorCode) {
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Email already in use"
                    "ERROR_WEAK_PASSWORD" -> "Password is too weak"
                    "ERROR_INVALID_CREDENTIALS" -> "Invalid email or password"
                    "ERROR_USER_NOT_FOUND" -> "User not found"
                    "ERROR_WRONG_PASSWORD" -> "Wrong password"
                    else -> e.message ?: "Authentication error"
                }
            }
            else -> {
                Log.w(tag, "mapError: General Exception caught: ${e.localizedMessage}", e)
                e.message ?: "Network error. Please try again."
            }
        }
        return errorMsg
    }
}
