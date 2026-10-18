package com.ntando.ivu.network

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.ntando.ivu.data.prefs.PreferenceManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

private const val TAG = "IVU_FCM"

/**
 * Manager handling FCM device push notification token retrieval, registration with the backend REST API,
 * and deletion on sign-out.
 *
 * @param context Application context.
 */
class FcmTokenManager(private val context: Context) {

    private val preferenceManager = PreferenceManager(context)

    /**
     * Fetches the current FCM push token from [FirebaseMessaging] and registers it with the backend server.
     * Non-fatal: Failures are caught and logged without throwing exceptions or blocking authentication flows.
     *
     * @param reminderEnabled Optional reminder enabled override boolean.
     * @param reminderHour Optional reminder hour (0-23) override integer.
     */
    suspend fun registerCurrentToken(reminderEnabled: Boolean? = null, reminderHour: Int? = null) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.d(TAG, "registerCurrentToken skipped: No active Firebase user session")
            return
        }

        try {
            Log.d(TAG, "Fetching FCM token from FirebaseMessaging.getInstance().token...")
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "Fetched FCM token: $token")
            preferenceManager.setFcmToken(token)

            val isEnabled = reminderEnabled ?: preferenceManager.isRemindersEnabled.first()
            val hour = reminderHour ?: preferenceManager.reminderHour.first()

            val request = RegisterTokenRequest(
                token = token,
                reminderEnabled = isEnabled,
                reminderHour = hour
            )

            Log.d(TAG, "Registering token with backend: reminderEnabled=$isEnabled, reminderHour=$hour...")
            val response = ApiClient.apiService.registerNotificationToken(request)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.i(TAG, "FCM token successfully registered with backend server!")
            } else {
                Log.e(TAG, "Backend token registration returned error: code=${response.code()}, error=${response.body()?.error}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Non-fatal error caught while fetching or registering FCM token", e)
        }
    }

    /**
     * Deletes the FCM token from backend server when user signs out.
     * Non-fatal: Failures are caught and logged.
     */
    suspend fun deleteTokenOnSignOut() {
        try {
            Log.d(TAG, "Deleting FCM token from backend server on sign-out...")
            val response = ApiClient.apiService.deleteNotificationToken()
            if (response.isSuccessful) {
                Log.i(TAG, "FCM token deleted successfully from backend on sign-out")
            } else {
                Log.w(TAG, "Delete notification token returned HTTP code=${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Non-fatal error deleting FCM token on sign-out", e)
        }
    }
}
