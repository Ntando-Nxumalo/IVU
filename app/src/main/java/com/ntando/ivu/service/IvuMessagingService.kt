package com.ntando.ivu.service

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ntando.ivu.R
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.network.ApiClient
import com.ntando.ivu.network.RegisterTokenRequest
import com.ntando.ivu.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "IVU_FCM"

/**
 * Firebase Cloud Messaging service handling FCM token registration and incoming push message displays.
 */
class IvuMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken received: token=$token")

        val preferenceManager = PreferenceManager(applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                preferenceManager.setFcmToken(token)
                Log.d(TAG, "FCM token saved to DataStore")

                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    val reminderEnabled = preferenceManager.isRemindersEnabled.first()
                    val reminderHour = preferenceManager.reminderHour.first()
                    val request = RegisterTokenRequest(
                        token = token,
                        reminderEnabled = reminderEnabled,
                        reminderHour = reminderHour
                    )
                    Log.d(TAG, "Registering FCM token with backend server...")
                    val response = ApiClient.apiService.registerNotificationToken(request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        Log.i(TAG, "FCM token successfully registered with backend server")
                    } else {
                        Log.e(TAG, "Failed to register FCM token with server HTTP code=${response.code()}")
                    }
                } else {
                    Log.d(TAG, "No signed-in user found. Skipping token backend registration.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception caught during token refresh registration", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "onMessageReceived from: ${remoteMessage.from}, messageId: ${remoteMessage.messageId}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: getString(R.string.app_name)

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "Time to study and keep your streak going!"

        Log.d(TAG, "Extracted notification payload - title: '$title', body: '$body'")

        val badgeName = remoteMessage.data["badgeName"] ?: "First Steps"
        if (title.contains("Badge", ignoreCase = true) || body.contains("Badge", ignoreCase = true) || remoteMessage.data.containsKey("badgeName")) {
            NotificationHelper.showBadgeUnlockedNotification(this, badgeName)
        } else {
            NotificationHelper.showNotification(this, title, body)
        }
    }
}
