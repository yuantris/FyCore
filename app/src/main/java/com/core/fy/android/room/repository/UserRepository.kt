package com.core.fy.android.room.repository

import com.core.fy.android.room.appDb
import com.core.fy.android.room.dao.UserDao
import com.core.fy.android.room.entity.User
import io.core.common.base.room.RoomRepository

object UserRepository : RoomRepository<User, UserDao>(appDb.userDao()) {

    suspend fun getUserByName(name: String) = queryByCondition("name = ?", listOf(name))
    suspend fun getUserById(id: Long) = queryByCondition("id = ?", listOf(id))
    suspend fun deleteAll() = dao.clear()

}