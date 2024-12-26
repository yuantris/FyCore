package com.core.fy.android.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = User.TABLE_NAME,
    indices = [Index(value = ["name"], unique = true)] // 设置 name 字段唯一
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var name: String,
    var age: Int,
) {
    companion object {
        const val TABLE_NAME = "user_table"
    }
}
