package com.core.libraries.base.room

import androidx.sqlite.db.SimpleSQLiteQuery
import com.core.libraries.base.ext.logD

abstract class RoomRepository<T : Any, D : BaseDao<T>>(val dao: D) {

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
        "当前表名: ${dao.getTableName()}".logD()
        return dao.getAll(SimpleSQLiteQuery("SELECT * FROM ${dao.getTableName()}"))
    }
}


