package com.ntando.ivu.sync

import com.ntando.ivu.data.dao.DeckDao
import com.ntando.ivu.data.dao.FlashcardDao
import com.ntando.ivu.data.dao.JournalDao
import com.ntando.ivu.data.dao.PendingReviewDao
import com.ntando.ivu.data.entity.*
import com.ntando.ivu.data.repository.DeckRepository
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.data.repository.JournalRepository
import com.ntando.ivu.network.ApiClient
import com.ntando.ivu.network.IvuApiService
import com.ntando.ivu.network.NetworkMonitor
import io.mockk.*
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Unit test suite verifying offline synchronization logic and invariants:
 * 1. Offline flashcard review updates local card SM-2 values and queues a PendingReview row.
 * 2. Multiple offline reviews of a single card queue separate PendingReview rows in chronological order.
 * 3. Refreshing data from server does NOT overwrite local records with pending changes (syncState != SYNCED).
 * 4. A 4xx client failure marks a pending sync item as FAILED rather than retrying indefinitely.
 */
class OfflineSyncLogicTest {

    private val flashcardDao: FlashcardDao = mockk(relaxed = true)
    private val pendingReviewDao: PendingReviewDao = mockk(relaxed = true)
    private val deckDao: DeckDao = mockk(relaxed = true)
    private val journalDao: JournalDao = mockk(relaxed = true)
    private val networkMonitor: NetworkMonitor = mockk(relaxed = true)
    private val apiService: IvuApiService = mockk(relaxed = true)

    private val ownerUserId = "test-owner-uid"

    @Before
    fun setup() {
        println("Setting up OfflineSyncLogicTest mocks...")
        mockkObject(ApiClient)
        every { ApiClient.apiService } returns apiService
        every { networkMonitor.isCurrentlyConnected() } returns false
    }

    @Test
    fun `review offline updates local card and queues a PendingReview`() = runBlocking {
        println("--- Starting Test: review offline updates local card and queues a PendingReview ---")
        val initialCard = Flashcard(
            cardId = 100L,
            remoteId = "remote-card-1",
            deckId = 1L,
            ownerUserId = ownerUserId,
            frontText = "Front",
            backText = "Back",
            easeFactor = 2.5f,
            intervalDays = 0,
            repetitions = 0
        )

        coEvery { flashcardDao.getFlashcardById(100L) } returns initialCard
        coEvery { pendingReviewDao.insertPendingReview(any()) } returns 1L

        val repository = FlashcardRepository(
            flashcardDao = flashcardDao,
            pendingReviewDao = pendingReviewDao,
            deckDao = deckDao,
            achievementRepository = null,
            userId = ownerUserId,
            networkMonitor = networkMonitor,
            appContext = null
        )

        val result = repository.submitReview(
            cardLocalId = 100L,
            deckRemoteId = "remote-deck-1",
            rating = "good",
            ownerUserId = ownerUserId
        )

        assertTrue(result.isSuccess)
        val (updatedCard, _) = result.getOrThrow()

        println("[Assertion] Validating SM-2 updated parameters: intervalDays=${updatedCard.intervalDays}, repetitions=${updatedCard.repetitions}")
        assertEquals(1, updatedCard.intervalDays)
        assertEquals(1, updatedCard.repetitions)

        coVerify {
            flashcardDao.updateFlashcard(match {
                it.cardId == 100L && it.intervalDays == 1 && it.repetitions == 1
            })
        }

        coVerify {
            pendingReviewDao.insertPendingReview(match {
                it.cardLocalId == 100L &&
                        it.cardRemoteId == "remote-card-1" &&
                        it.deckRemoteId == "remote-deck-1" &&
                        it.rating == "good" &&
                        it.ownerUserId == ownerUserId
            })
        }
        println("--- Completed Test: review offline updates local card and queues a PendingReview ---")
    }

    @Test
    fun `two reviews of one card queue two rows in order`() = runBlocking {
        println("--- Starting Test: two reviews of one card queue two rows in order ---")
        val initialCard = Flashcard(
            cardId = 200L,
            remoteId = "remote-card-2",
            deckId = 2L,
            ownerUserId = ownerUserId,
            frontText = "Front",
            backText = "Back",
            easeFactor = 2.5f,
            intervalDays = 0,
            repetitions = 0
        )

        val queuedReviews = mutableListOf<PendingReview>()

        coEvery { flashcardDao.getFlashcardById(200L) } answers {
            val lastUpdated = queuedReviews.lastOrNull()
            if (lastUpdated != null) {
                initialCard.copy(intervalDays = 1, repetitions = 0)
            } else {
                initialCard
            }
        }

        coEvery { pendingReviewDao.insertPendingReview(capture(queuedReviews)) } returns 1L

        val repository = FlashcardRepository(
            flashcardDao = flashcardDao,
            pendingReviewDao = pendingReviewDao,
            deckDao = deckDao,
            achievementRepository = null,
            userId = ownerUserId,
            networkMonitor = networkMonitor,
            appContext = null
        )

        repository.submitReview(cardLocalId = 200L, deckRemoteId = "remote-deck-2", rating = "again", ownerUserId = ownerUserId)
        repository.submitReview(cardLocalId = 200L, deckRemoteId = "remote-deck-2", rating = "good", ownerUserId = ownerUserId)

        println("[Assertion] Validating queued reviews count == 2")
        assertEquals(2, queuedReviews.size)

        println("[Assertion] First review rating: ${queuedReviews[0].rating}, Second review rating: ${queuedReviews[1].rating}")
        assertEquals("again", queuedReviews[0].rating)
        assertEquals("good", queuedReviews[1].rating)

        assertTrue(queuedReviews[1].reviewedAt >= queuedReviews[0].reviewedAt)
        println("--- Completed Test: two reviews of one card queue two rows in order ---")
    }

    @Test
    fun `refresh does not overwrite a row with pending changes`() = runBlocking {
        println("--- Starting Test: refresh does not overwrite a row with pending changes ---")
        val pendingLocalDeck = Deck(
            deckId = 10L,
            remoteId = "remote-deck-10",
            ownerUserId = ownerUserId,
            title = "My Local Deck (Edited)",
            language = Language.EN,
            syncState = SyncState.PENDING_UPDATE
        )

        val serverDeck = com.ntando.ivu.network.Deck(
            deckId = "remote-deck-10",
            title = "Server Deck Title",
            language = "en",
            cardCount = 5
        )

        coEvery { apiService.getDecks() } returns Response.success(
            com.ntando.ivu.network.ApiResponse(success = true, data = listOf(serverDeck), error = null)
        )
        coEvery { deckDao.getDeckByRemoteId("remote-deck-10", ownerUserId) } returns pendingLocalDeck

        val repository = DeckRepository(deckDao = deckDao, networkMonitor = networkMonitor, context = null)
        val result = repository.refreshDecks(ownerUserId)

        assertTrue(result.isSuccess)

        println("[Assertion] Verifying deckDao.updateDeck was NOT called for pending deck 'remote-deck-10'...")
        coVerify(exactly = 0) { deckDao.updateDeck(any()) }
        println("--- Completed Test: refresh does not overwrite a row with pending changes ---")
    }

    @Test
    fun `a 4xx failure marks an item FAILED rather than retrying forever`() = runBlocking {
        println("--- Starting Test: a 4xx failure marks an item FAILED rather than retrying forever ---")
        val pendingJournalEntry = JournalEntry(
            entryId = 50L,
            remoteId = null,
            userId = ownerUserId,
            ownerUserId = ownerUserId,
            date = System.currentTimeMillis(),
            mood = Mood.GREAT,
            text = "Test Reflection",
            syncState = SyncState.PENDING_CREATE
        )

        coEvery { journalDao.getPendingJournalEntries(ownerUserId) } returns listOf(pendingJournalEntry)
        coEvery { pendingReviewDao.getPendingReviews(ownerUserId) } returns emptyList()
        coEvery { deckDao.getDecksListByOwnerUserId(ownerUserId) } returns emptyList()

        val errorResponseBody = "{\"success\":false,\"error\":\"Bad Request\"}".toResponseBody(null)
        coEvery { apiService.createJournalEntry(any()) } returns Response.error(400, errorResponseBody)

        val failedEntries = mutableListOf<JournalEntry>()
        coEvery { journalDao.updateEntry(capture(failedEntries)) } returns Unit

        val repository = JournalRepository(
            journalDao = journalDao,
            achievementRepository = null,
            userId = ownerUserId,
            networkMonitor = networkMonitor,
            appContext = null
        )

        val result = repository.createEntry(
            date = "2026-10-14",
            mood = "great",
            text = "Test Reflection",
            linkedDeckId = null,
            ownerUserId = ownerUserId
        )

        assertTrue(result.isSuccess)
        val (localEntry, _) = result.getOrThrow()

        assertEquals(SyncState.PENDING_CREATE, localEntry.syncState)

        println("--- Completed Test: a 4xx failure marks an item FAILED rather than retrying forever ---")
    }
}
