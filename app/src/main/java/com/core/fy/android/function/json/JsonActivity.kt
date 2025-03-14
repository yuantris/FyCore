package com.core.fy.android.function.json

import android.os.Bundle
import com.core.fy.android.databinding.ActivityJsonBinding
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.helper.JsonUltra
import io.core.common.util.extensions.ui.invisible
import io.core.common.util.extensions.ui.onDebouncedClick
import io.core.common.util.extensions.ui.toast
import io.core.common.util.extensions.ui.visible

/**
 * ██╗  ██╗███████╗██╗   ██╗    ┌──────────┐
 * ╚██╗██╔╝██╔════╝╚██╗ ██╔╝    │ 加载进度 │▰▰▰▰▰▰▰▰◯ 87%
 *  ╚███╔╝ █████╗   ╚████╔╝     └──────────┘
 *  ██╔██╗ ██╔══╝    ╚██╔╝      ╱╲▲△△△△△△△△
 * ██╔╝ ██╗██╗        ██║       ▉ ▏正在渲染配置矩阵...
 * ╚═╝  ╚═╝╚═╝        ╚═╝       ╲╱▼▽▽▽▽▽▽▽▽
 * 注释的艺术，正在生成......
 * 最后编译阶段 ████████░░░░ 65% (按 F12 解锁彩蛋)
 * @description
 * @author [Yuan]
 * 2025/3/14 14:42
 */
class JsonActivity : ReflectBindingActivity<ActivityJsonBinding>() {

    private var value: JsonUltra? = null
    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        with(binding) {
            process.onDebouncedClick {
                if (editJson.text.isNotBlank()) {
                    value = JsonUltra.parse(editJson.text.toString())
                } else toast("请输入json字符串")

            }
            getAllPath.onDebouncedClick {
                value?.let {
                    if (editJson.text.isNotBlank()) {
                        result.visible()
                        showResult.text = it.getAllPaths().toString()
                    } else toast("请输入json字符串")
                } ?: run { toast("请先解析json字符串") }
            }
            jsonValue.onDebouncedClick {
                value?.let {
                    if (inputPath.text.isNotBlank()) {
                        it[inputPath.text.toString()]?.asString()?.let { json ->
                            result.visible()
                            showResult.text = json
                        } ?: run {
                            result.invisible()
                            showResult.text = ""
                            toast("未取到值")
                        }
                    } else toast("请输入待取值的json路径")
                } ?: run { toast("请先解析json字符串") }
            }
        }
    }
}