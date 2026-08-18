package com.ntando.ivu.data.repository

import com.ntando.ivu.data.dao.DeckDao
import com.ntando.ivu.data.entity.Deck
import com.ntando.ivu.data.entity.Language
import com.ntando.ivu.network.ApiClient
import com.ntando.ivu.network.CreateDeckRequest
import kotlinx.coroutines.flow.Flow

class DeckRepository(private val deckDao: DeckDao) {

    fun getLocalDecks(userId: Long): Flow<List<Deck>> = deckDao.getDecksByUser(userId)

    suspend fun refreshDecks(userId: Long) {
        try {
            val response = ApiClient.apiService.getDecks()
            if (response.isSuccessful) {
                val networkDecks = response.body()?.data ?: emptyList()
                val entities = networkDecks.map { networkDeck ->
                    Deck(
                        remoteId = networkDeck.deckId,
                        ownerId = userId,
                        title = networkDeck.title,
                        language = mapStringToLanguage(networkDeck.language),
                        cardCount = networkDeck.cardCount
                    )
                }
                // Syncing logic: Insert or update based on remoteId
                // For a student project, we'll keep it simple: insert them.
                // Room's @Insert(onConflict = OnConflictStrategy.REPLACE) will handle it if we manage IDs correctly.
                entities.forEach { deckDao.insertDeck(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun createDeck(userId: Long, title: String, language: String): Boolean {
        return try {
            val request = CreateDeckRequest(title, language)
            val response = ApiClient.apiService.createDeck(request)
            if (response.isSuccessful) {
                response.body()?.data?.let { networkDeck ->
                    val entity = Deck(
                        remoteId = networkDeck.deckId,
                        ownerId = userId,
                        title = networkDeck.title,
                        language = mapStringToLanguage(networkDeck.language),
                        cardCount = networkDeck.cardCount
                    )
                    deckDao.insertDeck(entity)
                }
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    private fun mapStringToLanguage(lang: String): Language {
        return when (lang.lowercase()) {
            "zu" -> Language.ZU
            "af" -> Language.AF
            else -> Language.EN
        }
    }
}
