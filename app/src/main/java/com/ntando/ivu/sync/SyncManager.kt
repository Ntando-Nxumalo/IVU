package com.ntando.ivu.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val TAG = "IVU_SYNC"
private const val UNIQUE_WORK_NAME = "IvuSyncWorker"

/**
 * Singleton manager utility for enqueuing unique background [SyncWorker] tasks.
 */
object SyncManager {

    /**
     * Enqueues a unique [SyncWorker] request with network connection constraints
     * and exponential backoff retry policy.
     *
     * @param context Application or activity context.
     */
    fun enqueueSync(context: Context) {
        Log.d(TAG, "SyncManager: enqueueSync requested")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10000,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
        Log.i(TAG, "SyncManager: Enqueued unique SyncWorker task with CONNECTED constraint")
    }
}
