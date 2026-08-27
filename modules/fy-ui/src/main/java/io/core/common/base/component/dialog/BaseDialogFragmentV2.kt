package io.core.common.base.component.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.LayoutRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import io.core.ui.R
import io.core.common.util.DiveGestureLine
import io.core.common.util.extensions.ui.setStatusBarTextColor
import java.lang.reflect.ParameterizedType

abstract class BaseDialogFragmentV2<VB : ViewBinding>(
    @LayoutRes private val layoutRes: Int
) : DialogFragment() {

    private var _binding: VB? = null
    protected val binding: VB get() = _binding!!

    // 动画相关
    private var dialogAnimation: Int? = null

    // 主题样式
    open val themeResId: Int = R.style.BaseDialogTheme

    // 默认宽度比例
    open val widthRatio: Float = 0.8f

    // 默认最大高度比例
    open val maxHeightRatio: Float = 0.8f

    // 是否允许点击外部取消
    open val isCancelableOutside: Boolean = true

    // 是否显示全屏
    open val isFullScreen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置主题
        setStyle(STYLE_NO_TITLE, themeResId)
        applyAnimation(DialogAnimation.DEFAULT)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 使用 ContextThemeWrapper 确保主题一致
        val contextThemeWrapper = ContextThemeWrapper(requireContext(), themeResId)
        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        // 通过反射获取ViewBinding的bind方法
        val vbClass = (javaClass.genericSuperclass as ParameterizedType)
            .actualTypeArguments[0] as Class<*>
        val bindMethod = vbClass.getMethod(
            "bind",
            View::class.java
        )

        // 使用传入的layoutRes创建视图
        val rootView = localInflater.inflate(layoutRes, container, false)
        _binding = bindMethod.invoke(null, rootView) as VB
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCanceledOnTouchOutside(isCancelableOutside)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            DiveGestureLine.setImmerse(this)
            // 设置窗口参数
            val params = attributes
            if (isFullScreen) {
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                params.height = ViewGroup.LayoutParams.MATCH_PARENT
            } else {
                // 设置默认宽度为屏幕宽度的80%
                params.width = (resources.displayMetrics.widthPixels * widthRatio).toInt()
                // 设置最大高度
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                val maxHeight = (resources.displayMetrics.heightPixels * maxHeightRatio).toInt()
                binding.root.measure(
                    View.MeasureSpec.makeMeasureSpec(params.width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                if (binding.root.measuredHeight > maxHeight) {
                    params.height = maxHeight
                }

            }

            attributes = params

            // 设置动画
            dialogAnimation?.let {
                if (it == DialogAnimation.BOTTOM.animStyle) {
                    setGravity(Gravity.BOTTOM)
                }
                setWindowAnimations(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initData()
        initListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 初始化视图
    open fun initView() {}

    // 初始化数据
    open fun initData() {}

    // 初始化监听器
    open fun initListener() {}

    // 提供常用的动画效果
    enum class DialogAnimation(val animStyle: Int) {
        SCALE(R.style.ScaleAnimStyle),
        DEFAULT(R.style.IOSAnimStyle),
        BOTTOM(R.style.BottomAnimStyle),
    }

    // 应用预定义动画
    open fun applyAnimation(animation: DialogAnimation) {
        dialogAnimation = animation.animStyle
    }
}