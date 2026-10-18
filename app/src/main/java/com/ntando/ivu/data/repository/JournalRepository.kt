package com.ntando.ivu.data.repository

import android.content.Context
import android.util.Log
import com.ntando.ivu.data.dao.JournalDao
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.entity.Badge
import com.ntando.ivu.data.entity.JournalEntry
import com.ntando.ivu.data.entity.Mood
import com.ntando.ivu.data.entity.SyncState
import com.ntando.ivu.network.ApiClient
import com.ntando.ivu.network.CreateJournalRequest
import com.ntando.ivu.network.NetworkMonitor
import com.ntando.ivu.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Offline-first repository managing user journal reflection entries.
 *
 * Reads: Streamed directly from local Room database ([JournalDao]).
 * Writes: Immediate local insertion with [SyncState.PENDING_CREATE], followed by async sync attempt.
 */
class JournalRepository(
    private var journalDao: JournalDao? = null,
    private val achievementRepository: AchievementRepository? = null,
    private val userId: String = "",
    private var networkMonitor: NetworkMonitor? = null,
    private val appContext: Context? = null
) {
    private val TAG = "JournalRepository"

    constructor(context: Context, achievementRepository: AchievementRepository? = null) : this(
        journalDao = DatabaseProvider.getDatabase(context).journalDao(),
        achievementRepository = achievementRepository,
        userId = "",
        networkMonitor = NetworkMonitor(context),
        appContext = context
    )

    constructor(achievementRepository: AchievementRepository?, userId: String) : this(
        journalDao = null,
        achievementRepository = achievementRepository,
        userId = userId,
        networkMonitor = null,
        appContext = null
    )

    constructor(achievementRepository: AchievementRepository?, userId: String, context: Context) : this(
        journalDao = DatabaseProvider.getDatabase(context).journalDao(),
        achievementRepository = achievementRepository,
        userId = userId,
        networkMonitor = NetworkMonitor(context),
        appContext = context
    )

    init {
        if (appContext != null) {
            if (journalDao == null) journalDao = DatabaseProvider.getDatabase(appContext).journalDao()
            if (networkMonitor == null) networkMonitor = NetworkMonitor(appContext)
        }
    }

    /**
     * Observes user journal reflection entries from local Room database.
     *
     * @param ownerUserId Firebase UID string of user.
     * @return [Flow] emitting list of [JournalEntry] objects.
     */
    fun getJournalEntries(ownerUserId: String): Flow<List<JournalEntry>> {
        Log.d(TAG, "getJournalEntries: Observing entries for ownerUserId='$ownerUserId'")
        return journalDao?.getEntriesByOwnerUserId(ownerUserId) ?: flowOf(emptyList())
    }

    /**
     * Refreshes journal entries from the remote backend server when online.
     *
     * Does NOT overwrite local records with pending changes (`syncState != SYNCED`).
     * Deletes local synced entries that no longer exist on the server.
     *
     * @param ownerUserId Firebase UID string of user.
     * @return [Result] wrapping [Unit].
     */
    suspend fun refreshEntries(ownerUserId: String): Result<Unit> {
        val dao = journalDao ?: return Result.failure(IllegalStateException("JournalDao not initialized"))
        Log.d(TAG, "refreshEntries: Initiating journal entries refresh for ownerUserId='$ownerUserId'")
        return try {
            val response = ApiClient.apiService.getJournalEntries()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    val remoteEntries = body.data
                    val serverRemoteIds = mutableListOf<String>()

                    for (remote in remoteEntries) {
                        val remoteId = remote.entryId ?: continue
                        serverRemoteIds.add(remoteId)

                        val existing = dao.getEntryByRemoteId(remoteId, ownerUserId)
                        val moodVal = parseMood(remote.mood)
                        val dateMillis = parseDateStringToMillis(remote.date)

                        if (existing == null) {
                            val newEntry = JournalEntry(
                                remoteId = remoteId,
                                userId = ownerUserId,
                                ownerUserId = ownerUserId,
                                date = dateMillis,
                                mood = moodVal,
                                text = remote.text,
                                syncState = SyncState.SYNCED,
                                updatedAt = System.currentTimeMillis()
                            )
                            dao.insertEntry(newEntry)
                        } else if (existing.syncState == SyncState.SYNCED) {
                            val updated = existing.copy(
                                date = dateMillis,
                                mood = moodVal,
                                text = remote.text,
                                updatedAt = System.currentTimeMillis()
                            )
                            dao.updateEntry(updated)
                        }
                    }

                    dao.deleteSyncedJournalEntriesNotIn(serverRemoteIds, ownerUserId)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(body?.error ?: "Unknown error"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()} error"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "refreshEntries: Network exception caught during journal refresh (swallowed for offline mode)", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a new journal entry locally in Room with `PENDING_CREATE` and the correct selected date.
     * Enqueues immediate online submission if connected; returns newly unlocked badges.
     */
    suspend fun createEntry(
        date: String,
        mood: String,
        text: String,
        linkedDeckId: String?,
        ownerUserId: String
    ): Result<Pair<JournalEntry, List<Badge>>> {
        val dao = journalDao ?: return Result.failure(IllegalStateException("JournalDao not initialized"))
        Log.d(TAG, "createEntry: Creating offline-first journal entry for date='$date', mood='$mood'")
        val dateMillis = parseDateStringToMillis(date)
        val moodVal = parseMood(mood)
        val linkedDeckLocalId = linkedDeckId?.toLongOrNull()

        var localEntry = JournalEntry(
            userId = ownerUserId,
            ownerUserId = ownerUserId,
            date = dateMillis,
            mood = moodVal,
            text = text,
            linkedDeckId = linkedDeckLocalId,
            syncState = SyncState.PENDING_CREATE,
            updatedAt = System.currentTimeMillis()
        )

        val newId = dao.insertEntry(localEntry)
        localEntry = localEntry.copy(entryId = newId)
        Log.i(TAG, "createEntry: Inserted journal entry locally in Room with entryId=$newId, syncState=PENDING_CREATE")

        // Trigger SyncWorker asynchronously
        appContext?.let { SyncManager.enqueueSync(it) }

        val unlockedBadges = achievementRepository?.recordJournalEntry(ownerUserId) ?: emptyList()

        return Result.success(Pair(localEntry, unlockedBadges))
    }

    // --- Legacy compatibility bridge ---

    suspend fun fetchEntries(ownerUserId: String = ""): Result<List<com.ntando.ivu.network.JournalEntry>> {
        refreshEntries(ownerUserId)
        return try {
            val response = ApiClient.apiService.getJournalEntries()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    suspend fun createEntry(
        date: String,
        mood: String,
        text: String,
        linkedDeckId: String?
    ): Result<Pair<com.ntando.ivu.network.JournalEntry, List<Badge>>> {
        val result = createEntry(date, mood, text, linkedDeckId, ownerUserId = userId)
        return if (result.isSuccess) {
            val (localEntry, badges) = result.getOrThrow()
            val netEntry = com.ntando.ivu.network.JournalEntry(
                entryId = localEntry.remoteId ?: localEntry.entryId.toString(),
                userId = localEntry.userId,
                date = date,
                mood = mood,
                text = localEntry.text,
                linkedDeckId = linkedDeckId
            )
            Result.success(Pair(netEntry, badges))
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Failed to create journal entry"))
        }
    }

    private fun parseMood(moodStr: String): Mood {
        return try {
            Mood.valueOf(moodStr.uppercase())
        } catch (e: Exception) {
            Mood.GREAT
        }
    }

    private fun parseDateStringToMillis(dateStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr)
            date?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                dateStr.toLong()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}
