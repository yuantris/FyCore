package com.core.fy.android.function

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.SizeUtils
import com.core.fy.android.R
import com.core.fy.android.databinding.ActivityDialogBinding
import com.core.fy.android.help.config.ReadBookConfig.textSize
import com.core.fy.android.ui.BottomSheetNextDialog
import com.core.fy.android.ui.WaitDialog
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.helper.dialogs.alert
import io.core.common.util.CoreUtil
import io.core.common.util.ext.cool.dpToPx
import io.core.common.util.ext.cool.spToPx
import io.core.common.util.ext.ui.getActivity
import io.core.common.util.ext.ui.getCompatColor
import io.core.common.util.ext.ui.onClick
import io.core.common.util.ext.ui.screenWidthDp
import io.core.common.util.ext.ui.showDialogFragment
import io.core.common.util.log.logD
import io.core.common.util.tools.DrawableBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2024/12/25 13:43
 * @description
 * @author Yuan
 */
class DialogActivity : ReflectBindingActivity<ActivityDialogBinding>() {
    private val dialog by lazy {
        WaitDialog.Builder(this)
    }

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        // 设置标题，左对齐
        binding.titleBar.apply {
            addRightButtonImage(R.drawable.ic_find_replace) {
                alert("温馨提示") {
                    setBackground(
                        DrawableBuilder
                            .setRadius(24f)
                            .setSolidColor(ctx.getCompatColor(R.color.md_cyan_50))
                            .build()
                    )
                    setCustomTitle(TextView(ctx).apply {
                        text = "温馨提示"
                        textSize = 24f  // 设置标题字体大小
                        setTextColor(Color.RED)  // 设置标题文字颜色
                        setPadding(24.dpToPx(), 20, 24.dpToPx(), 20)  // 设置标题的内边距
                    })
                    //setMessage("确定关闭吗？")
                    setCustomView(ImageView(ctx).apply {
                        setImageResource(R.drawable.splash_3)
                        scaleType = ImageView.ScaleType.FIT_XY
                    })
                    okButton {
                        CoreUtil.toast("已关闭")
                    }
                }.let {
                    val compatColor = context.getCompatColor(R.color.md_amber_A200)
                    it.getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(compatColor)
//                    // 修改消息文字属性
//                    it.findViewById<TextView>(android.R.id.message)?.apply {
//                        textSize = 18f   // 设置消息字体大小
//                        setTextColor(
//                            ContextCompat.getColor(
//                                context,
//                                R.color.md_blue_grey_400
//                            )
//                        )  // 设置消息文字颜色
//                        // 你可以设置其他字体属性（如字体、样式等）
//                    }
                }
            }
            setTitleStyle(Typeface.BOLD)
        }
    }

    override fun setListener() {
        super.setListener()
        binding.apply {
            show.onClick {
                lifecycleScope.launch {
                    dialog.setMessage("正在加载中").show()
                    delay(2000)
                    dialog.setMessage("加载完成")
                    delay(3000)
                    dialog.dismiss()
                }
            }

            dismiss.onClick {
                dialog.dismiss()
            }

            bottom.onClick {
                showDialogFragment<BottomSheetNextDialog>()
                //BottomSheetLaterDialog(this@DialogActivity).show()
            }
        }
    }
}