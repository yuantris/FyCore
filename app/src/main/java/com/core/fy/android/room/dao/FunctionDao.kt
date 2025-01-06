package com.core.fy.android.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.core.fy.android.room.entity.Function
import com.core.libraries.base.room.BaseDao

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
    override fun getTableName(): String = Function.TABLE_NAME

    @Query("SELECT * FROM function_table ORDER BY position ASC")
    fun getFunctionList(): List<Function>?
}