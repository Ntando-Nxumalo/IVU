package com.ntando.ivu.data.repository

import android.content.Context
import android.util.Log
import com.ntando.ivu.data.dao.DeckDao
import com.ntando.ivu.data.dao.FlashcardDao
import com.ntando.ivu.data.dao.PendingReviewDao
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.entity.Badge
import com.ntando.ivu.data.entity.Flashcard
import com.ntando.ivu.data.entity.PendingReview
import com.ntando.ivu.data.entity.SyncState
import com.ntando.ivu.network.ApiClient
import com.ntando.ivu.network.CreateFlashcardRequest
import com.ntando.ivu.network.NetworkMonitor
import com.ntando.ivu.network.ReviewRequest
import com.ntando.ivu.sync.SyncManager
import com.ntando.ivu.util.SpacedRepetitionScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Offline-first repository for managing flashcards and spaced-repetition reviews.
 */
class FlashcardRepository(
    private var flashcardDao: FlashcardDao? = null,
    private var pendingReviewDao: PendingReviewDao? = null,
    private var deckDao: DeckDao? = null,
    private val achievementRepository: AchievementRepository? = null,
    private val userId: String = "",
    private var networkMonitor: NetworkMonitor? = null,
    private val appContext: Context? = null
) {
    private val TAG = "FlashcardRepository"

    constructor(context: Context, achievementRepository: AchievementRepository? = null) : this(
        flashcardDao = DatabaseProvider.getDatabase(context).flashcardDao(),
        pendingReviewDao = DatabaseProvider.getDatabase(context).pendingReviewDao(),
        deckDao = DatabaseProvider.getDatabase(context).deckDao(),
        achievementRepository = achievementRepository,
        userId = "",
        networkMonitor = NetworkMonitor(context),
        appContext = context
    )

    constructor(achievementRepository: AchievementRepository?, userId: String) : this(
        flashcardDao = null,
        pendingReviewDao = null,
        deckDao = null,
        achievementRepository = achievementRepository,
        userId = userId,
        networkMonitor = null,
        appContext = null
    )

    constructor(achievementRepository: AchievementRepository?, userId: String, context: Context) : this(
        flashcardDao = DatabaseProvider.getDatabase(context).flashcardDao(),
        pendingReviewDao = DatabaseProvider.getDatabase(context).pendingReviewDao(),
        deckDao = DatabaseProvider.getDatabase(context).deckDao(),
        achievementRepository = achievementRepository,
        userId = userId,
        networkMonitor = NetworkMonitor(context),
        appContext = context
    )

    init {
        if (appContext != null) {
            if (flashcardDao == null) flashcardDao = DatabaseProvider.getDatabase(appContext).flashcardDao()
            if (pendingReviewDao == null) pendingReviewDao = DatabaseProvider.getDatabase(appContext).pendingReviewDao()
            if (deckDao == null) deckDao = DatabaseProvider.getDatabase(appContext).deckDao()
            if (networkMonitor == null) networkMonitor = NetworkMonitor(appContext)
        }
    }

    /**
     * Observes flashcards for a specific deck from local Room storage.
     */
    fun getCards(deckLocalId: Long, ownerUserId: String): Flow<List<Flashcard>> {
        Log.d(TAG, "getCards: Observing cards for deckLocalId=$deckLocalId, ownerUserId='$ownerUserId'")
        return flashcardDao?.getFlashcardsByDeckAndOwner(deckLocalId, ownerUserId) ?: flowOf(emptyList())
    }

    /**
     * Observes due flashcards for review in a specific deck from Room.
     */
    fun getDueCards(deckLocalId: Long, ownerUserId: String): Flow<List<Flashcard>> {
        val currentTime = System.currentTimeMillis()
        Log.d(TAG, "getDueCards: Observing due cards for deckLocalId=$deckLocalId, currentTime=$currentTime")
        return flashcardDao?.getDueFlashcardsByOwner(deckLocalId, currentTime, ownerUserId) ?: flowOf(emptyList())
    }

    /**
     * Refreshes flashcards for a deck from remote server when online.
     */
    suspend fun refreshCards(deckLocalId: Long, deckRemoteId: String, ownerUserId: String): Result<Unit> {
        val fDao = flashcardDao ?: return Result.failure(IllegalStateException("FlashcardDao not initialized"))
        val dDao = deckDao ?: return Result.failure(IllegalStateException("DeckDao not initialized"))

        Log.d(TAG, "refreshCards: Initiating refresh for deckLocalId=$deckLocalId, deckRemoteId='$deckRemoteId'")
        return try {
            val response = ApiClient.apiService.getCards(deckRemoteId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    val remoteCards = body.data
                    val serverRemoteIds = mutableListOf<String>()

                    for (remote in remoteCards) {
                        val cardRemoteId = remote.cardId ?: continue
                        serverRemoteIds.add(cardRemoteId)

                        val existing = fDao.getFlashcardByRemoteId(cardRemoteId, ownerUserId)
                        val easeFloat = remote.easeFactor.toFloat()

                        if (existing == null) {
                            val newCard = Flashcard(
                                remoteId = cardRemoteId,
                                deckId = deckLocalId,
                                ownerUserId = ownerUserId,
                                frontText = remote.frontText,
                                backText = remote.backText,
                                imageUrl = remote.imageUrl,
                                easeFactor = easeFloat,
                                intervalDays = remote.intervalDays,
                                repetitions = remote.repetitions,
                                dueDate = remote.dueDate,
                                syncState = SyncState.SYNCED,
                                updatedAt = System.currentTimeMillis()
                            )
                            fDao.insertFlashcard(newCard)
                        } else if (existing.syncState == SyncState.SYNCED) {
                            val updatedCard = existing.copy(
                                frontText = remote.frontText,
                                backText = remote.backText,
                                imageUrl = remote.imageUrl,
                                easeFactor = easeFloat,
                                intervalDays = remote.intervalDays,
                                repetitions = remote.repetitions,
                                dueDate = remote.dueDate,
                                updatedAt = System.currentTimeMillis()
                            )
                            fDao.updateFlashcard(updatedCard)
                        }
                    }

                    fDao.deleteSyncedFlashcardsNotIn(deckLocalId, serverRemoteIds, ownerUserId)
                    dDao.updateCardCount(deckLocalId)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(body?.error ?: "Unknown error"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()} error"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "refreshCards: Network exception caught during cards refresh (swallowed for offline mode)", e)
            Result.failure(e)
        }
    }

    /**
     * Submits a flashcard review rating locally immediately via SM-2 calculation and queues a PendingReview row.
     */
    suspend fun submitReview(
        cardLocalId: Long,
        deckRemoteId: String,
        rating: String,
        ownerUserId: String
    ): Result<Pair<Flashcard, List<Badge>>> {
        val fDao = flashcardDao ?: return Result.failure(IllegalStateException("FlashcardDao not initialized"))
        val pDao = pendingReviewDao ?: return Result.failure(IllegalStateException("PendingReviewDao not initialized"))

        Log.d(TAG, "submitReview: Executing offline-first SM-2 review for cardLocalId=$cardLocalId, rating='$rating'")
        val localCard = fDao.getFlashcardById(cardLocalId)
            ?: return Result.failure(IllegalArgumentException("Flashcard not found locally with cardId=$cardLocalId"))

        // 1. Calculate updated SM-2 parameters locally
        val locallyUpdatedCard = SpacedRepetitionScheduler.calculateNextReview(localCard, rating)
        fDao.updateFlashcard(locallyUpdatedCard)

        // 2. Queue PendingReview row
        val pendingReview = PendingReview(
            cardLocalId = cardLocalId,
            cardRemoteId = localCard.remoteId,
            deckRemoteId = deckRemoteId,
            rating = rating,
            reviewedAt = System.currentTimeMillis(),
            ownerUserId = ownerUserId
        )
        val pendingId = pDao.insertPendingReview(pendingReview)

        // Enqueue background sync worker
        appContext?.let { SyncManager.enqueueSync(it) }

        // 3. Award achievement progress
        val unlockedBadges = achievementRepository?.recordReview(ownerUserId) ?: emptyList()

        // 4. Instant sync if online and card has remote ID
        var finalCard = locallyUpdatedCard
        val isOnline = networkMonitor?.isCurrentlyConnected() ?: true
        if (isOnline && localCard.remoteId != null) {
            try {
                val response = ApiClient.apiService.reviewCard(
                    deckId = deckRemoteId,
                    cardId = localCard.remoteId,
                    request = ReviewRequest(rating)
                )
                if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                    val serverCard = response.body()!!.data!!
                    pDao.deletePendingReview(pendingId)

                    finalCard = locallyUpdatedCard.copy(
                        easeFactor = serverCard.easeFactor.toFloat(),
                        intervalDays = serverCard.intervalDays,
                        repetitions = serverCard.repetitions,
                        dueDate = serverCard.dueDate,
                        syncState = SyncState.SYNCED,
                        updatedAt = System.currentTimeMillis()
                    )
                    fDao.updateFlashcard(finalCard)
                }
            } catch (e: Exception) {
                Log.w(TAG, "submitReview: Immediate online review call failed. Kept PendingReview row $pendingId queued for background sync.", e)
            }
        }

        return Result.success(Pair(finalCard, unlockedBadges))
    }

    /**
     * Creates a new flashcard in Room locally with `PENDING_CREATE`. Attempts online creation if connected.
     */
    suspend fun createCard(
        deckLocalId: Long,
        deckRemoteId: String?,
        front: String,
        back: String,
        ownerUserId: String
    ): Result<Flashcard> {
        val fDao = flashcardDao ?: return Result.failure(IllegalStateException("FlashcardDao not initialized"))
        val dDao = deckDao ?: return Result.failure(IllegalStateException("DeckDao not initialized"))

        var localCard = Flashcard(
            deckId = deckLocalId,
            ownerUserId = ownerUserId,
            frontText = front,
            backText = back,
            syncState = SyncState.PENDING_CREATE,
            updatedAt = System.currentTimeMillis()
        )
        val newCardId = fDao.insertFlashcard(localCard)
        localCard = localCard.copy(cardId = newCardId)
        dDao.updateCardCount(deckLocalId)

        // Enqueue background sync worker
        appContext?.let { SyncManager.enqueueSync(it) }

        val isOnline = networkMonitor?.isCurrentlyConnected() ?: true
        if (isOnline && deckRemoteId != null) {
            try {
                val response = ApiClient.apiService.createCard(
                    deckId = deckRemoteId,
                    request = CreateFlashcardRequest(frontText = front, backText = back)
                )
                if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                    val serverData = response.body()!!.data!!
                    localCard = localCard.copy(
                        remoteId = serverData.cardId,
                        syncState = SyncState.SYNCED,
                        updatedAt = System.currentTimeMillis()
                    )
                    fDao.updateFlashcard(localCard)
                }
            } catch (e: Exception) {
                Log.w(TAG, "createCard: Online card creation failed, card kept as PENDING_CREATE locally", e)
            }
        }

        return Result.success(localCard)
    }

    /**
     * Deletes a flashcard from local Room DB and attempts server deletion if online.
     */
    suspend fun deleteCard(
        cardLocalId: Long,
        cardRemoteId: String?,
        deckLocalId: Long,
        deckRemoteId: String?,
        ownerUserId: String
    ): Result<Unit> {
        val isOnline = networkMonitor?.isCurrentlyConnected() ?: true
        if (isOnline && cardRemoteId != null && deckRemoteId != null) {
            try {
                ApiClient.apiService.deleteCard(deckRemoteId, cardRemoteId)
            } catch (e: Exception) {
                Log.w(TAG, "deleteCard: Server delete failed for cardRemoteId=$cardRemoteId", e)
            }
        }

        flashcardDao?.deleteFlashcardById(cardLocalId)
        deckDao?.updateCardCount(deckLocalId)
        return Result.success(Unit)
    }

    // --- Legacy compatibility overloads ---

    suspend fun fetchAllCards(deckId: String, ownerUserId: String = ""): Result<List<com.ntando.ivu.network.Flashcard>> {
        val deckLocalId = deckId.toLongOrNull() ?: 0L
        refreshCards(deckLocalId, deckId, ownerUserId)
        return try {
            val response = ApiClient.apiService.getCards(deckId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    suspend fun fetchDueCards(deckId: String, ownerUserId: String = ""): Result<List<com.ntando.ivu.network.Flashcard>> {
        val deckLocalId = deckId.toLongOrNull() ?: 0L
        refreshCards(deckLocalId, deckId, ownerUserId)
        return try {
            val response = ApiClient.apiService.getDueCards(deckId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    suspend fun submitReview(
        deckId: String,
        cardId: String,
        rating: String,
        ownerUserId: String = ""
    ): Result<Pair<com.ntando.ivu.network.Flashcard, List<Badge>>> {
        val cardLocalId = cardId.toLongOrNull() ?: 0L
        val result = submitReview(cardLocalId, deckId, rating, ownerUserId)
        return if (result.isSuccess) {
            val (localCard, badges) = result.getOrThrow()
            val netCard = com.ntando.ivu.network.Flashcard(
                cardId = localCard.remoteId ?: cardId,
                frontText = localCard.frontText,
                backText = localCard.backText,
                imageUrl = localCard.imageUrl,
                easeFactor = localCard.easeFactor.toDouble(),
                intervalDays = localCard.intervalDays,
                repetitions = localCard.repetitions,
                dueDate = localCard.dueDate
            )
            Result.success(Pair(netCard, badges))
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Review failed"))
        }
    }

    suspend fun createCard(deckId: String, front: String, back: String, ownerUserId: String = ""): Result<com.ntando.ivu.network.Flashcard> {
        val deckLocalId = deckId.toLongOrNull() ?: 0L
        val result = createCard(deckLocalId, deckId, front, back, ownerUserId)
        return if (result.isSuccess) {
            val localCard = result.getOrThrow()
            val netCard = com.ntando.ivu.network.Flashcard(
                cardId = localCard.remoteId,
                frontText = localCard.frontText,
                backText = localCard.backText,
                imageUrl = localCard.imageUrl,
                easeFactor = localCard.easeFactor.toDouble(),
                intervalDays = localCard.intervalDays,
                repetitions = localCard.repetitions,
                dueDate = localCard.dueDate
            )
            Result.success(netCard)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Failed to create card"))
        }
    }

    suspend fun deleteCard(deckId: String, cardId: String, ownerUserId: String = ""): Result<Unit> {
        val cardLocalId = cardId.toLongOrNull() ?: 0L
        val deckLocalId = deckId.toLongOrNull() ?: 0L
        return deleteCard(cardLocalId, cardId, deckLocalId, deckId, ownerUserId)
    }
}
