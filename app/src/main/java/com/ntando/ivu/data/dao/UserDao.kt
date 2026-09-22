package com.ntando.ivu.data.dao

import androidx.room.*
import com.ntando.ivu.data.entity.User
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for executing database operations on [User] profile records.
 */
@Dao
interface UserDao {

    /**
     * Inserts a new user entity into the database.
     *
     * @param user The [User] profile entity to insert.
     * @return Auto-generated primary key user ID.
     */
    @Insert
    suspend fun insertUser(user: User): Long

    /**
     * Updates an existing user profile record.
     *
     * @param user The updated [User] entity.
     */
    @Update
    suspend fun updateUser(user: User)

    /**
     * Streams user record matching specified primary key ID.
     *
     * @param id Primary key ID of user.
     * @return A [Flow] emitting the matching [User], or `null` if not found.
     */
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: Long): Flow<User?>

    /**
     * Queries user record by exact display name.
     *
     * @param name Name string to search.
     * @return The matching [User] entity, or `null`.
     */
    @Query("SELECT * FROM users WHERE name = :name")
    suspend fun getUserByName(name: String): User?

    /**
     * Queries user record by exact email address.
     *
     * @param email Email address string to search.
     * @return The matching [User] entity, or `null`.
     */
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

    /**
     * Streams all registered users stored in database.
     *
     * @return A [Flow] emitting list of all [User] entities.
     */
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>
}
