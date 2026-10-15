package com.ntando.ivu.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result as WorkResult
import androidx.work.WorkerParameters
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.entity.SyncState
import com.ntando.ivu.data.repository.DeckRepository
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.data.repository.JournalRepository
import com.ntando.ivu.network.ApiClient
import com.ntando.ivu.network.CreateJournalRequest
import com.ntando.ivu.network.ReviewRequest
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "IVU_SYNC"

/**
 * Background WorkManager worker executing offline queue synchronization and server refreshes.
 *
 * Processes in order:
 * 1. Pending journal entries (`POST /journal`).
 * 2. Pending card reviews (`PUT /decks/:deckId/cards/:cardId/review`).
 * 3. Server data refresh for decks, flashcards, and journal entries into local Room DB.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): WorkResult {
        Log.d(TAG, "SyncWorker started execution")

        val sharedPref = applicationContext.getSharedPreferences("IVUPrefs", Context.MODE_PRIVATE)
        val ownerUserId = sharedPref.getString("firebase_uid", "") ?: ""

        if (ownerUserId.isBlank()) {
            Log.d(TAG, "SyncWorker cancelled: No authenticated user session found (ownerUserId is blank)")
            return WorkResult.success()
        }

        val db = DatabaseProvider.getDatabase(applicationContext)
        val journalDao = db.journalDao()
        val pendingReviewDao = db.pendingReviewDao()
        val flashcardDao = db.flashcardDao()
        val deckDao = db.deckDao()

        val deckRepository = DeckRepository(applicationContext)
        val flashcardRepository = FlashcardRepository(applicationContext)
        val journalRepository = JournalRepository(applicationContext)

        try {
            // ----------------------------------------------------
            // 1. Process Pending Journal Entries
            // ----------------------------------------------------
            val pendingJournalEntries = journalDao.getPendingJournalEntries(ownerUserId)
            Log.d(TAG, "Found ${pendingJournalEntries.size} pending journal entries to sync for ownerUserId='$ownerUserId'")

            for (entry in pendingJournalEntries) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dateStr = sdf.format(Date(entry.date))

                    // Resolve linked deck remote ID if present
                    val linkedDeckRemoteId = entry.linkedDeckId?.let { localDeckId ->
                        deckDao.getDeckById(localDeckId)?.remoteId
                    }

                    val request = CreateJournalRequest(
                        date = dateStr,
                        mood = entry.mood.name.lowercase(),
                        text = entry.text,
                        linkedDeckId = linkedDeckRemoteId
                    )

                    Log.d(TAG, "Posting pending journal entry localId=${entry.entryId}, date='$dateStr'")
                    val response = ApiClient.apiService.createJournalEntry(request)

                    if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                        val serverData = response.body()!!.data!!
                        val updatedEntry = entry.copy(
                            remoteId = serverData.entryId,
                            syncState = SyncState.SYNCED,
                            updatedAt = System.currentTimeMillis()
                        )
                        journalDao.updateEntry(updatedEntry)
                        Log.i(TAG, "Journal entry localId=${entry.entryId} synced successfully. Assigned remoteId='${serverData.entryId}'")
                    } else {
                        val code = response.code()
                        handleError(code, "Journal entry localId=${entry.entryId}") {
                            val failedEntry = entry.copy(syncState = SyncState.FAILED)
                            journalDao.updateEntry(failedEntry)
                        }
                    }
                } catch (e: Exception) {
                    return handleException(e, "Journal entry localId=${entry.entryId}")
                }
            }

            // ----------------------------------------------------
            // 2. Process Pending Reviews (Oldest first)
            // ----------------------------------------------------
            val pendingReviews = pendingReviewDao.getPendingReviews(ownerUserId)
            Log.d(TAG, "Found ${pendingReviews.size} pending reviews to sync for ownerUserId='$ownerUserId'")

            for (review in pendingReviews) {
                try {
                    val cardRemoteId = review.cardRemoteId
                        ?: flashcardDao.getFlashcardById(review.cardLocalId)?.remoteId

                    if (cardRemoteId.isNullOrBlank()) {
                        Log.w(TAG, "Pending review id=${review.id} skipped: card has no remoteId yet")
                        continue
                    }

                    // Resolve deck remote ID
                    var deckRemoteId = review.deckRemoteId
                    val localDeck = deckDao.getDeckById(review.deckRemoteId.toLongOrNull() ?: -1L)
                    if (localDeck?.remoteId != null) {
                        deckRemoteId = localDeck.remoteId
                    }

                    Log.d(TAG, "Syncing pending review id=${review.id} for cardRemoteId='$cardRemoteId' in deckRemoteId='$deckRemoteId'")
                    val response = ApiClient.apiService.reviewCard(
                        deckId = deckRemoteId,
                        cardId = cardRemoteId,
                        request = ReviewRequest(review.rating)
                    )

                    if (response.isSuccessful && response.body()?.success == true) {
                        pendingReviewDao.deletePendingReview(review.id)
                        Log.i(TAG, "Pending review id=${review.id} synced and removed from queue")
                    } else {
                        val code = response.code()
                        handleError(code, "Pending review id=${review.id}") {
                            pendingReviewDao.deletePendingReview(review.id)
                        }
                    }
                } catch (e: Exception) {
                    return handleException(e, "Pending review id=${review.id}")
                }
            }

            // ----------------------------------------------------
            // 3. Refresh Server Data into Room DB
            // ----------------------------------------------------
            Log.d(TAG, "Executing background server refreshes...")
            deckRepository.refreshDecks(ownerUserId)
            journalRepository.refreshEntries(ownerUserId)

            val localDecks = deckDao.getDecksListByOwnerUserId(ownerUserId)
            for (deck in localDecks) {
                if (deck.remoteId != null) {
                    flashcardRepository.refreshCards(deck.deckId, deck.remoteId, ownerUserId)
                }
            }

            Log.i(TAG, "SyncWorker completed execution successfully")
            return WorkResult.success()

        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker caught top-level exception", e)
            return WorkResult.retry()
        }
    }

    private suspend fun handleError(
        httpCode: Int,
        itemTag: String,
        onMarkFailed: suspend () -> Unit
    ) {
        if (httpCode == 401 || httpCode == 408 || httpCode == 429) {
            Log.e(TAG, "$itemTag temporary HTTP $httpCode error. Will retry.")
        } else if (httpCode in 400..499) {
            Log.e(TAG, "$itemTag unrecoverable 4xx client error HTTP $httpCode. Marking FAILED.")
            onMarkFailed()
        } else {
            Log.e(TAG, "$itemTag server error HTTP $httpCode.")
        }
    }

    private fun handleException(e: Exception, itemTag: String): WorkResult {
        if (e is IOException || e is HttpException) {
            Log.e(TAG, "$itemTag network/IO exception caught. Scheduling work retry.", e)
            return WorkResult.retry()
        } else {
            Log.e(TAG, "$itemTag unexpected exception caught", e)
            return WorkResult.retry()
        }
    }
}
