package com.ntando.ivu.sync

import android.content.Context
import com.ntando.ivu.data.database.DatabaseProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Utility repository computing total pending offline changes waiting to be synchronized.
 */
class SyncStatusRepository(context: Context) {
    private val db = DatabaseProvider.getDatabase(context.applicationContext)

    /**
     * Streams combined total count of pending records (journal entries, reviews, flashcards, decks).
     *
     * @param ownerUserId Firebase UID string of user.
     * @return [Flow] emitting total count of pending offline changes.
     */
    fun getPendingCountFlow(ownerUserId: String): Flow<Int> {
        return combine(
            db.journalDao().getPendingJournalEntriesCountFlow(ownerUserId),
            db.pendingReviewDao().getPendingReviewCountFlow(ownerUserId),
            db.flashcardDao().getPendingFlashcardCountFlow(ownerUserId),
            db.deckDao().getPendingDeckCountFlow(ownerUserId)
        ) { journalCount, reviewCount, cardCount, deckCount ->
            journalCount + reviewCount + cardCount + deckCount
        }
    }
}
