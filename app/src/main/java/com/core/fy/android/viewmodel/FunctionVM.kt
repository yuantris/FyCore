package com.core.fy.android.viewmodel

import androidx.lifecycle.MutableLiveData
import com.core.fy.android.room.entity.Function
import com.core.fy.android.room.repository.FunctionRepository
import com.core.libraries.base.ext.logD
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

    val data: MutableLiveData<List<Function>> = MutableLiveData()

    enum class Design(val function: String) {
        KEYBOARD("键盘"),
        ROOM("room"),
        DIALOG("dialog"),
        TOAST("toast"),
        VIEW_VISIBILITY("viewVisibility"),
        EVENT("event"),
        COLL_BAR("collBar");

        companion object {
            private val map: Map<String, Design> = values().associateBy { it.function }

            fun fromFunction(function: String): Design {
                return map[function] ?: KEYBOARD
            }
        }
    }

    val list by lazy {
        listOf(
            Function(Design.KEYBOARD),
            Function(Design.ROOM),
            Function(Design.DIALOG),
            Function(Design.TOAST),
            Function(Design.VIEW_VISIBILITY),
            Function(Design.EVENT),
            Function(Design.COLL_BAR),
        )
    }

    fun initRvData() {
        launch(
            block = {
                val allList = repository.getAllList() ?: run {
                    repository.dao.insertAll(list)
                    repository.getAllList()
                }
                if ((allList?.size ?: 0) < list.size) {
                    list.forEachIndexed { index, function ->
                        val functionWithDesign =
                            repository.dao.getFunctionWithDesign(function.design.function)
                        functionWithDesign?.let {
                            it.position = index
                            repository.update(it)
                        } ?: run {
                            repository.insert(function)
                        }
                    }
                } else if ((allList?.size ?: 0) > list.size) {
                    val onlyInAllList =
                        allList?.filter { it.design !in list.map { bean -> bean.design } }
                    onlyInAllList?.forEach {
                        repository.delete(it)
                    }
                }
                allList
            },
            onSuccess = {
                // 更新列表
                data.postValue(it)
            },
            onError = {
                it.message?.logD()
            }
        )
    }
}