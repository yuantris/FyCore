package com.core.fy.android.room.repository

import com.core.fy.android.room.appDb
import com.core.fy.android.room.dao.FunctionDao
import com.core.fy.android.room.entity.Function
import io.core.common.base.room.RoomRepository

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
object FunctionRepository : RoomRepository<Function, FunctionDao>(appDb.functionDao()) {

    fun getAllList(): List<Function>? {
        return dao.getFunctionList()
    }

}