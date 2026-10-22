package com.ntando.ivu.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ntando.ivu.AchievementsActivity
import com.ntando.ivu.IVU
import com.ntando.ivu.R

/**
 * Utility helper class for creating notification channels and displaying local notifications.
 */
object NotificationHelper {
    /** Notification channel ID for study reminders and badge alerts. */
    const val CHANNEL_ID_REMINDERS = "ivu_reminders"
    private const val TAG = "IVU_FCM"

    /**
     * Creates the "ivu_reminders" notification channel on Android 8.0 (API level 26) and higher.
     *
     * @param context Application context.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Study reminders"
            val descriptionText = "Channel for IVU study reminders and achievement notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID_REMINDERS, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel '$CHANNEL_ID_REMINDERS' created/verified.")
        }
    }

    /**
     * Displays a local notification on the "ivu_reminders" channel if POST_NOTIFICATIONS permission is granted.
     *
     * @param context Context used to construct and show notification.
     * @param title Title text for notification.
     * @param body Body content text for notification.
     * @param targetIntent Optional custom intent to launch when notification is tapped.
     */
    fun showNotification(context: Context, title: String, body: String, targetIntent: Intent? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification skipped: POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        createNotificationChannel(context)

        val intent = targetIntent ?: Intent(context, IVU::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
        Log.i(TAG, "Displayed local notification on channel '$CHANNEL_ID_REMINDERS' (id=$notificationId): '$title' - '$body'")
    }

    /**
     * Displays a local notification celebrating an unlocked achievement badge.
     * Tapping it opens AchievementsActivity directly to display the unlocked badge details.
     *
     * @param context Application context.
     * @param badgeName Name of the unlocked badge.
     */
    fun showBadgeUnlockedNotification(context: Context, badgeName: String) {
        val intent = Intent(context, AchievementsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("UNLOCKED_BADGE_NAME", badgeName)
        }

        showNotification(
            context = context,
            title = context.getString(R.string.title_badge_unlocked),
            body = context.getString(R.string.body_badge_unlocked, badgeName),
            targetIntent = intent
        )
    }
}
