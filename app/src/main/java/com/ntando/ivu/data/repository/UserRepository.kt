package com.ntando.ivu.data.repository

import com.ntando.ivu.data.dao.UserDao
import com.ntando.ivu.data.entity.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository to manage [User] data for the IVU application.
 */
class UserRepository(private val userDao: UserDao) {

    fun getUserById(id: Long): Flow<User?> = userDao.getUserById(id)

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }
}
