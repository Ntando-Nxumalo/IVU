package com.ntando.ivu.data.repository

import com.ntando.ivu.data.dao.JournalDao
import com.ntando.ivu.data.entity.JournalEntry
import com.ntando.ivu.data.entity.Mood
import com.ntando.ivu.network.ApiClient
import com.ntando.ivu.network.CreateJournalRequest
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalDao: JournalDao) {

    fun getLocalEntries(userId: Long): Flow<List<JournalEntry>> = journalDao.getEntriesByUser(userId)

    suspend fun refreshJournalEntries(userId: Long) {
        try {
            val response = ApiClient.apiService.getJournalEntries()
            if (response.isSuccessful) {
                val networkEntries = response.body()?.data ?: emptyList()
                val entities = networkEntries.map { networkEntry ->
                    JournalEntry(
                        userId = userId,
                        date = networkEntry.date.toLongOrNull() ?: System.currentTimeMillis(),
                        mood = mapStringToMood(networkEntry.mood),
                        text = networkEntry.text,
                        linkedDeckId = networkEntry.linkedDeckId?.toLongOrNull()
                    )
                }
                entities.forEach { journalDao.insertEntry(it) }
            }
        } catch (e: Exception) {
            // Log error
        }
    }

    suspend fun createEntry(userId: Long, date: String, mood: String, text: String, deckId: String?): Boolean {
        return try {
            val request = CreateJournalRequest(date, mood, text, deckId)
            val response = ApiClient.apiService.createJournalEntry(request)
            if (response.isSuccessful) {
                response.body()?.data?.let { networkEntry ->
                    val entity = JournalEntry(
                        userId = userId,
                        date = networkEntry.date.toLongOrNull() ?: System.currentTimeMillis(),
                        mood = mapStringToMood(networkEntry.mood),
                        text = networkEntry.text,
                        linkedDeckId = networkEntry.linkedDeckId?.toLongOrNull()
                    )
                    journalDao.insertEntry(entity)
                }
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    private fun mapStringToMood(mood: String): Mood {
        return when (mood.lowercase()) {
            "great" -> Mood.GREAT
            "tough" -> Mood.TOUGH
            else -> Mood.OKAY
        }
    }
}
