package com.core.libraries.base.room

import androidx.sqlite.db.SimpleSQLiteQuery
import com.core.libraries.base.ext.logD

abstract class RoomRepository<T : Any, D : BaseDao<T>>(val dao: D) : BaseRepository() {

    suspend fun insert(item: T) {
        dao.insert(item)
    }

    suspend fun delete(item: T) {
        dao.delete(item)
    }

    suspend fun update(item: T) {
        dao.update(item)
    }

    suspend fun queryAll(): List<T> {
        if (dao.getTableName().isEmpty()) {
            throw IllegalArgumentException("table name is empty")
        }
        return dao.getAll(SimpleSQLiteQuery("SELECT * FROM ${dao.getTableName()}"))
    }

    suspend fun queryByCondition(condition: String, args: List<Any> = emptyList()): T? {
        if (dao.getTableName().isEmpty()) {
            throw IllegalArgumentException("Table name is empty. Ensure that getTableName() is properly implemented in ${dao::class.java.simpleName}.")
        }
        val query = "SELECT * FROM ${dao.getTableName()} WHERE $condition"
        "执行查询: $query".logD()
        return dao.findByCondition(SimpleSQLiteQuery(query, args.toTypedArray()))
    }
}


