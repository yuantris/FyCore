package com.core.fy.android.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.core.fy.android.MainActivity
import com.drake.brv.annotaion.ItemOrientation
import com.drake.brv.item.ItemDrag

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/6 15:40
 * @description
 * @author Yuan
 */
@Entity(tableName = Function.TABLE_NAME)
data class Function(
    var design: String = MainActivity.Design.KEYBOARD.function,
    override var itemOrientationDrag: Int = ItemOrientation.ALL
) : ItemDrag {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
    var position: Int = 0

    companion object {
        const val TABLE_NAME = "function_table"
    }
}