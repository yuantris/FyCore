package com.core.fy.android.room.repository

import com.core.fy.android.room.AppDatabase
import com.core.fy.android.room.dao.UserDao
import com.core.fy.android.room.entity.User
import com.core.libraries.base.room.RoomRepository

class UserRepository : RoomRepository<User, UserDao>(AppDatabase.getDatabase().userDao()) {

    suspend fun getUserByName(name: String) = dao.getUserByName(name)
    suspend fun getUserById(id: Long) = dao.getUserById(id)
    suspend fun deleteAll() = dao.deleteAll()

    companion object {
        private var instance: UserRepository? = null
        fun singletonCreate() = instance ?: synchronized(this) {
            instance ?: UserRepository().also { instance = it }
        }
    }
}