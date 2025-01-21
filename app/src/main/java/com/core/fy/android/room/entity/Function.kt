package com.core.fy.android.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.core.fy.android.MainActivity
import com.core.fy.android.viewmodel.FunctionVM
import io.core.engine.brv.annotaion.ItemOrientation
import io.core.engine.brv.item.ItemDrag
import kotlinx.parcelize.Parcelize

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
    var design: FunctionVM.Design = FunctionVM.Design.KEYBOARD,
    override var itemOrientationDrag: Int = ItemOrientation.ALL
) : ItemDrag {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
    var position: Int = 0

    companion object {
        const val TABLE_NAME = "function_table"
    }
}