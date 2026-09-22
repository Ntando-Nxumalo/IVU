package com.ntando.ivu.data.repository

import android.util.Log
import com.ntando.ivu.data.dao.UserDao
import com.ntando.ivu.data.entity.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository to manage local [User] entity persistence and retrieval for the IVU application.
 * Interacts directly with Room Database via [UserDao].
 *
 * @property userDao Data Access Object for local user table operations.
 */
class UserRepository(private val userDao: UserDao) {
    private val TAG = "UserRepository"

    /**
     * Retrieves a reactive [Flow] stream of user profile data by numerical user ID.
     *
     * @param id The unique database ID of the user.
     * @return [Flow] emitting the matching [User] or `null` if not found.
     */
    fun getUserById(id: Long): Flow<User?> {
        Log.d(TAG, "getUserById: Requesting user flow for id=$id")
        return userDao.getUserById(id)
    }

    /**
     * Inserts a new user record into the local database.
     *
     * @param user The [User] entity to insert.
     */
    suspend fun insertUser(user: User) {
        Log.d(TAG, "insertUser: Inserting user with id=${user.id}, email=${user.email}")
        try {
            userDao.insertUser(user)
            Log.i(TAG, "insertUser: User successfully inserted into DB with id=${user.id}")
        } catch (e: Exception) {
            Log.e(TAG, "insertUser: Failed to insert user with id=${user.id}", e)
            throw e
        }
    }

    /**
     * Updates an existing user record in the local database.
     *
     * @param user The [User] entity containing updated information.
     */
    suspend fun updateUser(user: User) {
        Log.d(TAG, "updateUser: Updating user with id=${user.id}")
        try {
            userDao.updateUser(user)
            Log.i(TAG, "updateUser: User successfully updated in DB for id=${user.id}")
        } catch (e: Exception) {
            Log.e(TAG, "updateUser: Failed to update user with id=${user.id}", e)
            throw e
        }
    }
}
