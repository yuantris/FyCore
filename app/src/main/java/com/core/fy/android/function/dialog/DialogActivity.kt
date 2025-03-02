package com.core.fy.android.function.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.core.fy.android.R
import com.core.fy.android.databinding.ActivityDialogBinding
import com.core.fy.android.ui.BottomSheetNextDialog
import com.core.fy.android.ui.WaitDialog
import com.core.fy.android.util.showDxMessage
import com.core.fy.android.util.showDxNotification
import com.core.fy.android.widget.buildSpannable
import com.core.fy.android.widget.enableLinkMovementMethod
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.base.component.dialog.BasePopup
import io.core.common.base.component.dialog.showCustomDialog
import io.core.common.base.component.dialog.specific.BubbleDialog
import io.core.common.helper.dialogs.showDialog
import io.core.common.util.CoreUtil
import io.core.common.util.extensions.cool.dpToPx
import io.core.common.util.extensions.cool.postDelayUI
import io.core.common.util.extensions.currentTimeMillis
import io.core.common.util.extensions.logD
import io.core.common.util.extensions.ui.getCompatColor
import io.core.common.util.extensions.ui.onClick
import io.core.common.util.extensions.ui.screenRealWidthPx
import io.core.common.util.extensions.ui.showDialogFragment
import io.core.common.util.extensions.ui.toast
import io.core.common.util.tools.DrawableBuilder
import io.core.common.util.tools.androidApiVersion
import io.core.common.util.tools.androidVersion
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

    private val dialog2 by lazy {
        BubbleDialog(this)
    }

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)

        // 设置标题，左对齐
        binding.titleBar.apply {
            addRightButtonImage(R.drawable.ic_find_replace) {
                showDialog("温馨提示") {
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
                }
            }
            setTitleStyle(Typeface.BOLD)
        }
//
//        val lineControlTextView = LineControlTextView(this)
//        lineControlTextView.apply {
//            addSingleLine("123")
//            addStyledLine("456") { spannable ->
//                spannable.setSpan(
//                    ForegroundColorSpan(Color.RED),
//                    0,
//                    spannable.length,
//                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//                )
//            }
//            addSingleLine("789")
//        }
        binding.lineText.apply {
            addSingleLine("123")
            addStyledLine("456") { spannable ->
                spannable.setSpan(
                    ForegroundColorSpan(Color.RED),
                    0,
                    spannable.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            addSingleLine("789")
        }

        binding.ct.apply {
            addSingleLine("Android $androidVersion")
            addSingleLine("Api $androidApiVersion")
        }

        //binding.span.movementMethod = LinkMovementMethod.getInstance()
        binding.span.apply {
            enableLinkMovementMethod()
            buildSpannable {
                text("Hello") {
                    bold()
                    color(Color.RED)
                    roundedBg(Color.LTGRAY, 12f)
                }

                text(" Kotlin") {
                    italic()
                    bold()
                    size(80)
                }

                text("\nClick Me") {
                    clickable(true) {
                        "Clicked!".logD()
                    }
                }

                text("\nVisit Google") {
                    url("https://www.google.com")
                    color(Color.BLUE)
                }
            }
        }
    }

    override fun setListener() {
        super.setListener()
        with(binding) {
            show.onClick {
                lifecycleScope.launch {
                    dialog.show()
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

            show2.onClick {
                dialog2.show()
                waitDismiss(dialog2)
            }

            show3.onClick {
                showCustomDialog {
                    setLayout(R.layout.dialog_bottom_street)
                    setSize((screenRealWidthPx * 0.8f).toInt(), 200.dpToPx())
                    setAnimation(BasePopup.PopupAnimation.FADE)
                    setViewInitializer { dialog ->

                    }
                    setOnDismissListener {
                        toast("已关闭")
                    }
                }
            }

            show4.onClick {
                showDxMessage {
                    content = "DialogX KongzueStyle show"
                    onMessageConfirm = { _, _ ->
                        showDxNotification {
                            content = currentTimeMillis.toString()
                        }
                    }
                }
            }
        }
    }

    /**
     * 等待2秒关闭对话框
     */
    private fun waitDismiss(dialog: Dialog) {
        postDelayUI(3000) {
            dialog.dismiss()
        }
    }
}