package com.core.fy.android.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.core.fy.android.room.entity.Function
import com.core.libraries.common.base.room.BaseDao

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/6 17:56
 * @description
 * @author Yuan
 */
@Dao
interface FunctionDao : BaseDao<Function> {

    @Query("SELECT * FROM ${Function.TABLE_NAME} ORDER BY position ASC")
    fun getFunctionList(): List<Function>?

    @Query("SELECT * FROM ${Function.TABLE_NAME} WHERE design = :design LIMIT 1")
    fun getFunctionWithDesign(design: String): Function?
}