package com.core.libraries.base.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface BaseDao<T : Any> {

    @Insert(onConflict = OnConflictStrategy.NONE)
    suspend fun insert(entity: T)

    @Insert(onConflict = OnConflictStrategy.NONE)
    suspend fun insertAll(entities: List<T>)

    @Update
    suspend fun update(entity: T)

    @Delete
    suspend fun delete(entity: T)

    // 获取所有数据
    @RawQuery
    suspend fun getAll(query: SupportSQLiteQuery): List<T>

    // 根据条件查询数据
    @RawQuery
    suspend fun findByCondition(query: SupportSQLiteQuery): T?

    fun getTableName(): String
}

