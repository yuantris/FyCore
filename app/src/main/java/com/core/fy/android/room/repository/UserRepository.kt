package com.core.fy.android.room.repository

import com.core.fy.android.room.AppDatabase
import com.core.fy.android.room.dao.UserDao
import com.core.fy.android.room.entity.User
import io.core.common.base.room.RoomRepository

class UserRepository : RoomRepository<User, UserDao>
    (AppDatabase.getInstance().userDao()) {

    suspend fun getUserByName(name: String) = queryByCondition("name = ?", listOf(name))
    suspend fun getUserById(id: Long) = queryByCondition("id = ?", listOf(id))
    suspend fun deleteAll() = dao.clear()

    companion object {
        private var instance: UserRepository? = null
        fun create() = instance ?: synchronized(this) {
            instance ?: UserRepository().also { instance = it }
        }
    }
}