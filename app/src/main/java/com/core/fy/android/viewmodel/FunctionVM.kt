package com.core.fy.android.viewmodel

import androidx.lifecycle.MutableLiveData
import com.core.fy.android.room.entity.Function
import com.core.fy.android.room.repository.FunctionRepository
import io.core.common.base.vm.BaseViewModel
import io.core.common.util.extensions.logD

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
        云创控件("云创控件"),
        单文字点击的TextView("单文字点击的TextView"),
        READ("开源阅读控件"),
        TTS("TTS"),
        ROOM("room"),
        DIALOG("dialog"),
        TOAST("toast"),
        VIEW_VISIBILITY("viewVisibility"),
        EVENT("event"),
        相机("相机"),
        单选多选("单选多选"),
        Brv("brv"),
        录音("录音"),
        Media("Media"),
        Json("Json"),
        FFmpeg("FFmpeg"),
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
            Function(Design.云创控件),
            Function(Design.单文字点击的TextView),
            Function(Design.READ),
            Function(Design.TTS),
            Function(Design.ROOM),
            Function(Design.DIALOG),
            Function(Design.TOAST),
            Function(Design.VIEW_VISIBILITY),
            Function(Design.EVENT),
            Function(Design.相机),
            Function(Design.单选多选),
            Function(Design.Brv),
            Function(Design.录音),
            Function(Design.Media),
            Function(Design.Json),
            Function(Design.FFmpeg),
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
                val allListSize = allList?.size ?: 0
                val designsInList = list.map { it.design }
                val designsInListSet = designsInList.toSet()
                when {
                    allListSize < list.size -> {
                        val functionsToInsert = mutableListOf<Function>()
                        val functionsToUpdate = mutableListOf<Function>()

                        list.forEachIndexed { index, function ->
                            val functionWithDesign =
                                repository.dao.getFunctionWithDesign(function.design.function)
                            if (functionWithDesign != null) {
                                functionWithDesign.position = index
                                functionsToUpdate.add(functionWithDesign)
                            } else {
                                functionsToInsert.add(function)
                            }
                        }

                        repository.dao.updateAll(functionsToUpdate)
                        repository.dao.insertAll(functionsToInsert)
                    }

                    allListSize > list.size -> {
                        val onlyInAllList = allList?.filter { it.design !in designsInListSet }
                        onlyInAllList?.let { repository.dao.deleteAll(it) }
                    }

                    else -> emptyList<Function>()
                }
                // 更新列表
                data.postValue(repository.getAllList())
            },

             error = {
                it.message?.logD()
            }
        )
    }
}