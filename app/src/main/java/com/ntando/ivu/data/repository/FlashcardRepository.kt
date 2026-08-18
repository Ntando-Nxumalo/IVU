package com.ntando.ivu.data.repository

import com.ntando.ivu.data.dao.FlashcardDao
import com.ntando.ivu.data.entity.Flashcard
import com.ntando.ivu.network.ApiClient
import com.ntando.ivu.network.CreateFlashcardRequest
import com.ntando.ivu.network.ReviewRequest
import kotlinx.coroutines.flow.Flow

class FlashcardRepository(private val flashcardDao: FlashcardDao) {

    fun getLocalFlashcards(deckId: Long): Flow<List<Flashcard>> = flashcardDao.getFlashcardsByDeck(deckId)

    suspend fun getFlashcardById(cardId: Long): Flashcard? = flashcardDao.getFlashcardById(cardId)

    suspend fun refreshFlashcards(remoteDeckId: String, localDeckId: Long) {
        try {
            val response = ApiClient.apiService.getCards(remoteDeckId)
            if (response.isSuccessful) {
                val networkCards = response.body()?.data ?: emptyList()
                val entities = networkCards.map { networkCard ->
                    Flashcard(
                        remoteId = networkCard.cardId,
                        deckId = localDeckId,
                        frontText = networkCard.frontText,
                        backText = networkCard.backText,
                        imageUrl = networkCard.imageUrl,
                        easeFactor = networkCard.easeFactor.toFloat(),
                        intervalDays = networkCard.intervalDays,
                        repetitions = networkCard.repetitions,
                        dueDate = networkCard.dueDate
                    )
                }
                entities.forEach { flashcardDao.insertFlashcard(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun reviewCard(remoteDeckId: String, remoteCardId: String, localCard: Flashcard, rating: String): Boolean {
        return try {
            val request = ReviewRequest(rating)
            val response = ApiClient.apiService.reviewCard(remoteDeckId, remoteCardId, request)
            if (response.isSuccessful) {
                response.body()?.data?.let { networkCard ->
                    val updatedEntity = localCard.copy(
                        easeFactor = networkCard.easeFactor.toFloat(),
                        intervalDays = networkCard.intervalDays,
                        repetitions = networkCard.repetitions,
                        dueDate = networkCard.dueDate
                    )
                    flashcardDao.updateFlashcard(updatedEntity)
                }
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createFlashcard(remoteDeckId: String, localDeckId: Long, front: String, back: String, image: String?): Boolean {
        return try {
            val request = CreateFlashcardRequest(front, back, image)
            val response = ApiClient.apiService.createCard(remoteDeckId, request)
            if (response.isSuccessful) {
                response.body()?.data?.let { networkCard ->
                    val entity = Flashcard(
                        remoteId = networkCard.cardId,
                        deckId = localDeckId,
                        frontText = networkCard.frontText,
                        backText = networkCard.backText,
                        imageUrl = networkCard.imageUrl,
                        easeFactor = networkCard.easeFactor.toFloat(),
                        intervalDays = networkCard.intervalDays,
                        repetitions = networkCard.repetitions,
                        dueDate = networkCard.dueDate
                    )
                    flashcardDao.insertFlashcard(entity)
                }
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateFlashcardLocal(flashcard: Flashcard) {
        flashcardDao.updateFlashcard(flashcard)
    }

    suspend fun deleteCard(remoteDeckId: String?, remoteCardId: String?, localCard: Flashcard): Boolean {
        return try {
            var apiSuccess = true
            if (remoteDeckId != null && remoteCardId != null) {
                val response = ApiClient.apiService.deleteCard(remoteDeckId, remoteCardId)
                apiSuccess = response.isSuccessful
            }
            
            if (apiSuccess) {
                flashcardDao.deleteFlashcard(localCard)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }
}
