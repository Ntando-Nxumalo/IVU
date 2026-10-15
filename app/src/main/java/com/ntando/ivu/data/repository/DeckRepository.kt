package com.ntando.ivu.data.repository

import android.content.Context
import android.util.Log
import com.ntando.ivu.data.dao.DeckDao
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.entity.Deck
import com.ntando.ivu.data.entity.Language
import com.ntando.ivu.data.entity.SyncState
import com.ntando.ivu.network.ApiClient
import com.ntando.ivu.network.CreateDeckRequest
import com.ntando.ivu.network.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Offline-first repository responsible for flashcard deck management.
 *
 * Reads: Streamed directly from local Room database ([DeckDao]).
 * Writes: Online-only operations for creation and deletion; refresh upserts server data to Room.
 */
class DeckRepository(
    private var deckDao: DeckDao? = null,
    private var networkMonitor: NetworkMonitor? = null,
    context: Context? = null
) {
    private val TAG = "DeckRepository"

    constructor(context: Context) : this(
        deckDao = DatabaseProvider.getDatabase(context).deckDao(),
        networkMonitor = NetworkMonitor(context),
        context = context
    )

    init {
        if (context != null) {
            if (deckDao == null) {
                deckDao = DatabaseProvider.getDatabase(context).deckDao()
            }
            if (networkMonitor == null) {
                networkMonitor = NetworkMonitor(context)
            }
        }
    }

    /**
     * Observes user flashcard decks stored in local Room database.
     *
     * @param ownerUserId Firebase UID string of user.
     * @return [Flow] emitting list of local [Deck] entities.
     */
    fun getDecks(ownerUserId: String): Flow<List<Deck>> {
        Log.d(TAG, "getDecks: Observing decks Flow for ownerUserId='$ownerUserId'")
        return deckDao?.getDecksByOwnerUserId(ownerUserId) ?: flowOf(emptyList())
    }

    /**
     * Refreshes local Room decks from the remote backend REST API when online.
     *
     * Does NOT overwrite local rows that have pending changes (`syncState != SYNCED`).
     * Deletes local synced rows that no longer exist on the server.
     *
     * @param ownerUserId Firebase UID string of user.
     * @return [Result] containing [Unit] on success or caught network exception.
     */
    suspend fun refreshDecks(ownerUserId: String): Result<Unit> {
        val dao = deckDao ?: return Result.failure(IllegalStateException("DeckDao not initialized"))
        Log.d(TAG, "refreshDecks: Initiating server refresh for ownerUserId='$ownerUserId'")
        return try {
            val response = ApiClient.apiService.getDecks()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    val remoteDecks = body.data
                    Log.i(TAG, "refreshDecks: Successfully fetched ${remoteDecks.size} remote decks")

                    val serverRemoteIds = mutableListOf<String>()

                    for (remote in remoteDecks) {
                        val remoteId = remote.deckId ?: continue
                        serverRemoteIds.add(remoteId)

                        val existing = dao.getDeckByRemoteId(remoteId, ownerUserId)
                        val lang = parseLanguage(remote.language)

                        if (existing == null) {
                            val newDeck = Deck(
                                remoteId = remoteId,
                                ownerUserId = ownerUserId,
                                title = remote.title,
                                language = lang,
                                cardCount = remote.cardCount,
                                syncState = SyncState.SYNCED,
                                updatedAt = System.currentTimeMillis()
                            )
                            dao.insertDeck(newDeck)
                            Log.d(TAG, "refreshDecks: Inserted new deck '$remoteId' into Room")
                        } else if (existing.syncState == SyncState.SYNCED) {
                            val updatedDeck = existing.copy(
                                title = remote.title,
                                language = lang,
                                cardCount = remote.cardCount,
                                updatedAt = System.currentTimeMillis()
                            )
                            dao.updateDeck(updatedDeck)
                            Log.d(TAG, "refreshDecks: Updated synced deck '$remoteId' in Room")
                        } else {
                            Log.d(TAG, "refreshDecks: Skipped overwrite for deck '$remoteId' with pending local state: ${existing.syncState}")
                        }
                    }

                    dao.deleteSyncedDecksNotIn(serverRemoteIds, ownerUserId)
                    Result.success(Unit)
                } else {
                    val errorMsg = body?.error ?: "Unknown error"
                    Log.w(TAG, "refreshDecks: API error response: '$errorMsg'")
                    Result.failure(Exception(errorMsg))
                }
            } else {
                Log.e(TAG, "refreshDecks: HTTP ${response.code()} error")
                Result.failure(Exception("HTTP ${response.code()} error"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "refreshDecks: Network exception caught during refresh (swallowed for offline mode)", e)
            Result.failure(e)
        }
    }

    /**
     * Legacy fetch method bridge.
     */
    suspend fun fetchDecks(ownerUserId: String = ""): Result<List<com.ntando.ivu.network.Deck>> {
        refreshDecks(ownerUserId)
        return try {
            val response = ApiClient.apiService.getDecks()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    /**
     * Creates a new deck online-only.
     */
    suspend fun createDeck(title: String, language: String, ownerUserId: String = ""): Result<com.ntando.ivu.network.Deck> {
        val isOnline = networkMonitor?.isCurrentlyConnected() ?: true
        if (!isOnline) {
            Log.w(TAG, "createDeck rejected: Device is offline")
            return Result.failure(IllegalStateException("You are offline. Creating decks requires an active internet connection."))
        }

        Log.d(TAG, "createDeck: Requesting online creation for title='$title', language='$language'")
        return try {
            val request = CreateDeckRequest(title, language)
            val response = ApiClient.apiService.createDeck(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    val remote = body.data
                    val localDeck = Deck(
                        remoteId = remote.deckId,
                        ownerUserId = ownerUserId,
                        title = remote.title,
                        language = parseLanguage(remote.language),
                        cardCount = remote.cardCount,
                        syncState = SyncState.SYNCED,
                        updatedAt = System.currentTimeMillis()
                    )
                    deckDao?.insertDeck(localDeck)
                    Result.success(remote)
                } else {
                    Result.failure(Exception(body?.error ?: "Failed to create deck"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "createDeck: Exception thrown creating deck", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a flashcard deck online-only.
     */
    suspend fun deleteDeck(deckId: String): Result<Unit> {
        val isOnline = networkMonitor?.isCurrentlyConnected() ?: true
        if (!isOnline) {
            Log.w(TAG, "deleteDeck rejected: Device is offline")
            return Result.failure(IllegalStateException("You are offline. Deleting decks requires an active internet connection."))
        }

        return try {
            val response = ApiClient.apiService.deleteDeck(deckId)
            if (response.isSuccessful && response.body()?.success == true) {
                val deckLocalId = deckId.toLongOrNull()
                if (deckLocalId != null) {
                    deckDao?.deleteDeckById(deckLocalId)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete deck on server"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteDeck: Exception thrown deleting deck", e)
            Result.failure(e)
        }
    }

    private fun parseLanguage(code: String): Language {
        return try {
            Language.valueOf(code.uppercase())
        } catch (e: Exception) {
            Language.EN
        }
    }
}
