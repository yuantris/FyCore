package com.core.fy.android.room

import androidx.room.TypeConverter
import com.core.fy.android.viewmodel.FunctionVM

class Converters {
    @TypeConverter
    fun fromDesign(bean: FunctionVM.Design): String {
        return bean.function // 枚举转字符串
    }

    @TypeConverter
    fun toDesign(value: String): FunctionVM.Design {
        return FunctionVM.Design.fromFunction(value) // 字符串转枚举
    }
}
