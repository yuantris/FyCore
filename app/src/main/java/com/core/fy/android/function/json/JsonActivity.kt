package com.core.fy.android.function.json

import android.os.Bundle
import com.core.fy.android.databinding.ActivityJsonBinding
import io.core.ui.base.component.activity.ReflectBindingActivity
import io.core.utils.JsonUltra
import io.core.utils.extensions.ui.invisible
import io.core.utils.extensions.ui.onDebouncedClick
import io.core.utils.extensions.ui.toast
import io.core.utils.extensions.ui.visible

/**
 * ██�? ██╗███████╗██╗   ██�?   ┌──────────�?
 * ╚██╗██╔╝██╔════╝╚██╗ ██╔╝    �?加载进度 │▰▰▰▰▰▰▰▰◯ 87%
 *  ╚███╔╝ █████╗   ╚████╔�?    └──────────�?
 *  ██╔██╗ ██╔══╝    ╚██╔�?     ╱╲▲△△△△△△△�?
 * ██╔╝ ██╗██╗        ██�?      �?▏正在渲染配置矩�?..
 * ╚═�? ╚═╝╚═╝        ╚═�?      ╲╱▼▽▽▽▽▽▽▽�?
 * 注释的艺术，正在生成......
 * 最后编译阶�?████████░░░░ 65% (�?F12 解锁彩蛋)
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
                } else toast("请输入json字符�?)

            }
            getAllPath.onDebouncedClick {
                value?.let {
                    if (editJson.text.isNotBlank()) {
                        result.visible()
                        showResult.text = it.getAllPaths().toString()
                    } else toast("请输入json字符�?)
                } ?: run { toast("请先解析json字符�?) }
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
                            toast("未取到�?)
                        }
                    } else toast("请输入待取值的json路径")
                } ?: run { toast("请先解析json字符�?) }
            }
        }
    }
}