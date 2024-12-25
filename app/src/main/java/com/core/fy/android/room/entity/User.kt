package com.core.fy.android.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = User.TABLE_NAME)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val age: Int,
) {
    companion object {
        const val TABLE_NAME = "user_table"
    }
}
