package com.core.fy.android.room.repository

import com.core.fy.android.room.AppDatabase
import com.core.fy.android.room.dao.FunctionDao
import com.core.fy.android.room.entity.Function
import com.core.libraries.common.base.room.RoomRepository

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
    RoomRepository<Function, FunctionDao>(AppDatabase.getInstance().functionDao()) {

    fun getAllList(): List<Function>? {
        return dao.getFunctionList()
    }

    companion object {
        private var instance: FunctionRepository? = null
        fun create() = instance ?: synchronized(this) {
            instance ?: FunctionRepository().also { instance = it }
        }
    }
}