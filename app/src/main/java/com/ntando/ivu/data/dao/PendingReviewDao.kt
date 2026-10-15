package com.ntando.ivu.data.dao

import androidx.room.*
import com.ntando.ivu.data.entity.PendingReview
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing queued offline flashcard reviews.
 */
@Dao
interface PendingReviewDao {
    /**
     * Inserts a pending review item into the offline queue.
     *
     * @param review The [PendingReview] record to queue.
     * @return Row primary key ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingReview(review: PendingReview): Long

    /**
     * Retrieves all pending reviews for a user ordered chronologically by review timestamp.
     *
     * @param ownerUserId Firebase UID string of user.
     * @return List of queued [PendingReview] items.
     */
    @Query("SELECT * FROM pending_reviews WHERE ownerUserId = :ownerUserId ORDER BY reviewedAt ASC")
    suspend fun getPendingReviews(ownerUserId: String): List<PendingReview>

    /**
     * Streams total count of pending reviews for a specific user.
     *
     * @param ownerUserId Firebase UID string of user.
     * @return [Flow] emitting count of pending review items.
     */
    @Query("SELECT COUNT(*) FROM pending_reviews WHERE ownerUserId = :ownerUserId")
    fun getPendingReviewCountFlow(ownerUserId: String): Flow<Int>

    /**
     * Deletes a processed pending review item by queue ID.
     *
     * @param id Primary key ID of the pending review record.
     */
    @Query("DELETE FROM pending_reviews WHERE id = :id")
    suspend fun deletePendingReview(id: Long)

    /**
     * Removes all pending reviews for a specific user ID.
     *
     * @param ownerUserId Firebase UID string of user.
     */
    @Query("DELETE FROM pending_reviews WHERE ownerUserId = :ownerUserId")
    suspend fun clearPendingReviews(ownerUserId: String)
}
