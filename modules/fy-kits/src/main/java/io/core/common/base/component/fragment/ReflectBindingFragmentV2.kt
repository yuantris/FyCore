package io.core.common.base.component.fragment

import android.view.View
import androidx.viewbinding.ViewBinding
import io.core.common.util.extensions.ui.inflateBindingWithGeneric

/**
 * ██╗  ██╗███████╗██╗   ██╗    ┌──────────┐
 * ╚██╗██╔╝██╔════╝╚██╗ ██╔╝    │ 加载进度 │▰▰▰▰▰▰▰▰◯ 87%
 *  ╚███╔╝ █████╗   ╚████╔╝     └──────────┘
 *  ██╔██╗ ██╔══╝    ╚██╔╝      ╱╲▲△△△△△△△△
 * ██╔╝ ██╗██╗        ██║       ▉ ▏正在渲染配置矩阵...
 * ╚═╝  ╚═╝╚═╝        ╚═╝       ╲╱▼▽▽▽▽▽▽▽▽
 * 注释的艺术，正在生成......
 * 最后编译阶段 ████████░░░░ 65% (按 F12 解锁彩蛋)
 * ---------------------------------------------
 *
 * @Author [Yuan]
 * 2025/4/30 8:50
 */
abstract class ReflectBindingFragmentV2<VB : ViewBinding> : BaseFragmentV2() {

    protected var _binding: VB? = null
    val binding: VB get() = _binding!!

    override fun contentViewBind(): View? {
        _binding = inflateBindingWithGeneric(layoutInflater, getFragmentContainer(), false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}