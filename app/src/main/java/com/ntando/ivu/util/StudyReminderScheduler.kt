package com.ntando.ivu.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ntando.ivu.data.prefs.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

private const val TAG = "IVU_REMINDER"
private const val ALARM_ACTION = "com.ntando.ivu.ACTION_DAILY_STUDY_REMINDER"

/**
 * BroadcastReceiver triggered daily by [AlarmManager] to display the study reminder notification.
 */
class StudyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "StudyReminderReceiver triggered with action: ${intent.action}")
        val preferenceManager = PreferenceManager(context.applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            val enabled = preferenceManager.isRemindersEnabled.first()
            if (enabled) {
                NotificationHelper.showNotification(
                    context = context.applicationContext,
                    title = "Study Reminder 📚",
                    body = "Time to study and keep your streak going! 🚀"
                )
            }
            // Reschedule for the next day at the same reminder hour
            StudyReminderScheduler.scheduleDailyReminder(context.applicationContext)
        }
    }
}

/**
 * Utility for scheduling daily local study reminder alarms via [AlarmManager].
 */
object StudyReminderScheduler {

    /**
     * Schedules or cancels daily local study reminder alarm based on DataStore preferences.
     *
     * @param context Application context.
     * @param enabled Optional override for reminder enabled status.
     * @param hour Optional override for reminder hour (0-23).
     */
    fun scheduleDailyReminder(context: Context, enabled: Boolean? = null, hour: Int? = null) {
        val preferenceManager = PreferenceManager(context.applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            val isEnabled = enabled ?: preferenceManager.isRemindersEnabled.first()
            val reminderHour = hour ?: preferenceManager.reminderHour.first()

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, StudyReminderReceiver::class.java).apply {
                action = ALARM_ACTION
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (!isEnabled) {
                Log.d(TAG, "Cancelling daily study reminder alarm")
                alarmManager.cancel(pendingIntent)
                return@launch
            }

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, reminderHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            Log.d(TAG, "Scheduling daily study reminder alarm for hour=$reminderHour:00 at ${calendar.time}")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }
}
