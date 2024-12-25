package com.core.fy.android.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.core.fy.android.room.entity.User
import com.core.libraries.base.room.BaseDao

@Dao
interface UserDao :BaseDao<User>{
    override fun getTableName(): String = User.TABLE_NAME

    @Query("SELECT * FROM user_table")
    suspend fun getAllUsers(): List<User>

    @Query("SELECT * FROM user_table WHERE id = :userId")
    suspend fun getUserById(userId: Long): User?

    @Query("SELECT * FROM user_table WHERE name = :name")
    suspend fun getUserByName(name: String): User

    @Query("DELETE FROM user_table")
    suspend fun deleteAll()

}