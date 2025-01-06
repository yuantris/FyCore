package com.core.fy.android.room.repository

import com.core.fy.android.room.AppDatabase
import com.core.fy.android.room.dao.FunctionDao
import com.core.fy.android.room.entity.Function
import com.core.libraries.base.room.RoomRepository

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/6 18:00
 * @description
 * @author Yuan
 */
class FunctionRepository :
    RoomRepository<Function, FunctionDao>(AppDatabase.getDatabase().functionDao()) {

    fun getAllList(): List<Function>? {
        return dao.getFunctionList()
    }

    companion object {
        private var instance: UserRepository? = null
        fun singletonCreate() = instance ?: synchronized(this) {
            instance ?: UserRepository().also { instance = it }
        }
    }
}