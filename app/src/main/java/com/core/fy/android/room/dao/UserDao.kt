package com.core.fy.android.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.core.fy.android.room.entity.User
import com.core.libraries.base.room.BaseDao

@Dao
interface UserDao : BaseDao<User> {
    override fun getTableName(): String = User.TABLE_NAME

    @Query("SELECT * FROM ${User.TABLE_NAME} WHERE name = :name")
    fun getUserSync(name: String): User

    @Query("DELETE FROM ${User.TABLE_NAME}")
    suspend fun clear()
}