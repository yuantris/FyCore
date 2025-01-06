package com.core.fy.android.viewmodel

import com.core.fy.android.MainActivity.Design
import com.core.fy.android.room.entity.Function
import com.core.fy.android.room.repository.FunctionRepository
import com.core.libraries.base.vm.BaseViewModel

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/6 18:03
 * @description
 * @author Yuan
 */
class FunctionVM(var repository: FunctionRepository) : BaseViewModel() {

    val list by lazy {
        listOf(
            Function(Design.KEYBOARD.function),
            Function(Design.ROOM.function),
            Function(Design.DIALOG.function),
            Function(Design.TOAST.function),
            Function(Design.VIEW_VISIBILITY.function),
            Function(Design.EVENT.function),
            Function(Design.COLL_BAR.function),
        )
    }

    fun getAllList() = async {
        repository.getAllList()
    }

    fun getFunctionWithDesign(design: String) = async {
        repository.dao.getFunctionWithDesign(design)
    }
}