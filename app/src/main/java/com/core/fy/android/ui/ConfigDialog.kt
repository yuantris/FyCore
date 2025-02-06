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
import io.core.common.helper.viewbindingdelegate.viewBinding
import io.core.common.util.ext.ui.applyTint
import io.core.common.util.ext.ui.getCompatColor
import io.core.common.util.ext.ui.restart
import io.core.common.util.ext.ui.setLayout
import io.core.common.util.log.LogCat
import io.core.common.util.log.logD
import io.core.common.util.tools.Preferences
import io.core.common.util.tools.TimeUtils


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
        binding.toolBar.setBackgroundColor(getCompatColor(io.core.R.color.md_grey_850))
        binding.toolBar.inflateMenu(io.core.R.menu.dialog_text)
        binding.toolBar.setTitleTextColor(getCompatColor(R.color.md_white_1000))
        binding.toolBar.menu.applyTint(requireContext())

        // 修改菜单项文字颜色
        val spannableString = SpannableString("关闭")
        spannableString.setSpan(
            ForegroundColorSpan(getCompatColor(R.color.md_white_1000)),
            0,
            spannableString.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.toolBar.menu.findItem(R.id.menu_close).setTitle(spannableString)
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                io.core.R.id.menu_close -> dismissAllowingStateLoss()
            }
            true
        }

        arguments?.let {
            binding.toolBar.title = "应用配置"
        }

        val isDisplaySplashAnim = Preferences.getValue(PreferKey.isDisplaySplashAnim, true)

        binding.apply {
            radioStartAnim.isChecked = isDisplaySplashAnim
            radioStartAnim.setOnClickListener {
                val check = Preferences.getValue(PreferKey.isDisplaySplashAnim, true)
                radioStartAnim.isChecked = !check
                Preferences.putValue(PreferKey.isDisplaySplashAnim, !check)
            }
        }
    }
}