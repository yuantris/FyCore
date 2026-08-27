package io.core.common.base.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface BaseDao<T : Any> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: T): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<T>): List<Long>

    @Update
    suspend fun update(entity: T): Int

    @Update
    suspend fun updateAll(entities: List<T>): Int

    @Delete
    suspend fun delete(entity: T): Int

    @Delete
    suspend fun deleteAll(entities: List<T>): Int

    // 获取所有数据
    @RawQuery
    suspend fun getAll(query: SupportSQLiteQuery): List<T>

    // 根据条件查询数据
    @RawQuery
    suspend fun findByCondition(query: SupportSQLiteQuery): T?

    // 执行任意 SQL
    @RawQuery
    suspend fun executeQuery(query: SupportSQLiteQuery): Int

    /**
     * 获取当前数据表的名称。子类必须重写此方法，
     * 返回该表的名称，以便执行动态的 SQL 查询操作。
     */
    @RequiresOverride
    fun getTableName(): String {
        throw NotImplementedError("Subclasses must override getTableName()")
    }
}

