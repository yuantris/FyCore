package com.core.libraries.base.room

import androidx.sqlite.db.SimpleSQLiteQuery
import com.core.libraries.base.ext.logD

abstract class RoomRepository<T : Any, D : BaseDao<T>>(val dao: D) : BaseRepository() {
    suspend fun queryAll(): List<T> {
        return dao.getAll(SimpleSQLiteQuery("SELECT * FROM ${dao.getTableName()}"))
    }

    suspend fun queryByCondition(condition: String, args: List<Any> = emptyList()): T? {
        val query = "SELECT * FROM ${dao.getTableName()} WHERE $condition"
        "执行查询: $query".logD()
        return dao.findByCondition(SimpleSQLiteQuery(query, args.toTypedArray()))
    }

    // 删除指定表（$tableName）中的所有数据，但不会删除表结构本身
    suspend fun delTable() {
        val deleteTableQuery = SimpleSQLiteQuery("DELETE FROM ${dao.getTableName()}")
        dao.executeQuery(deleteTableQuery)
    }
}


