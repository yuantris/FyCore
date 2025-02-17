package com.core.fy.android.ui

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import com.core.fy.android.R
import com.core.fy.android.constants.PreferKey
import com.core.fy.android.databinding.DialogAppConfigBinding
import io.core.common.base.component.dialog.BaseDialogFragment
import io.core.common.base.component.dialog.specific.CrashLogsDialog
import io.core.common.util.ext.cool.spanForeColor
import io.core.common.util.ext.ui.applyTint
import io.core.common.util.ext.ui.ctx
import io.core.common.util.ext.ui.getCompatColor
import io.core.common.util.ext.ui.onClick
import io.core.common.util.ext.ui.setLayout
import io.core.common.util.ext.ui.showDialogFragment
import io.core.common.util.ext.ui.viewBinding
import io.core.common.util.tools.Preferences


/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/2/5 17:23
 * @description
 * @author Yuan
 */
class ConfigDialog : BaseDialogFragment(R.layout.dialog_app_config) {

    private val binding by viewBinding(DialogAppConfigBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, 0.9f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(getCompatColor(R.color.md_grey_850))
        binding.toolBar.inflateMenu(R.menu.dialog_text)
        binding.toolBar.setTitleTextColor(getCompatColor(R.color.md_white_1000))
        binding.toolBar.menu.applyTint(ctx)

        // 修改菜单项文字颜色
        val whiteCloseString = "关闭".spanForeColor(getCompatColor(R.color.md_white_1000))
        binding.toolBar.menu.findItem(R.id.menu_close).setTitle(whiteCloseString)
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_close -> dismissAllowingStateLoss()
            }
            true
        }

        arguments?.let {
            binding.toolBar.title = "应用配置"
        }

        val isDisplaySplashAnim = Preferences.getValue(PreferKey.isDisplaySplashAnim, true)
        val isDisplayGuide = Preferences.getValue(PreferKey.isDisplayGuide, true)
        val isDisplayHomeSkeletonAnim = Preferences.getValue(PreferKey.isDisplayHomeSkeletonAnim, true)

        binding.apply {
            radioStartAnim.isChecked = isDisplaySplashAnim
            radioStartAnim.onClick {
                val check = Preferences.getValue(PreferKey.isDisplaySplashAnim, true)
                radioStartAnim.isChecked = !check
                Preferences.putValue(PreferKey.isDisplaySplashAnim, !check)
            }

            radioGuideShow.isChecked = isDisplayGuide
            radioGuideShow.onClick {
                val check = Preferences.getValue(PreferKey.isDisplayGuide, true)
                radioGuideShow.isChecked = !check
                Preferences.putValue(PreferKey.isDisplayGuide, !check)
            }

            radioHomeSkeletonAnim.isChecked = isDisplayHomeSkeletonAnim
            radioHomeSkeletonAnim.onClick {
                val check = Preferences.getValue(PreferKey.isDisplayHomeSkeletonAnim, true)
                radioHomeSkeletonAnim.isChecked = !check
                Preferences.putValue(PreferKey.isDisplayHomeSkeletonAnim, !check)
            }

            crumble.onClick {
                showDialogFragment<CrashLogsDialog>()
            }
        }
    }
}