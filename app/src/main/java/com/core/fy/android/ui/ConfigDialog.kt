package com.core.fy.android.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.core.fy.android.Config
import com.core.fy.android.R
import com.core.fy.android.constants.PreferKey
import com.core.fy.android.databinding.DialogAppConfigBinding
import io.core.ui.base.component.dialog.BaseDialogFragment
import io.core.ui.base.component.dialog.specific.CrashLogsDialog
import io.core.utils.extensions.cool.spanForeColor
import io.core.utils.extensions.ui.applyTint
import io.core.utils.extensions.ui.ctx
import io.core.utils.extensions.ui.getCompatColor
import io.core.utils.extensions.ui.onClick
import io.core.utils.extensions.ui.setLayout
import io.core.utils.extensions.ui.showDialogFragment
import io.core.utils.extensions.ui.viewBinding
import io.core.engine.storage.getWithAnnotation
import io.core.engine.storage.put
import io.core.engine.storage.storage


/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
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

        // 修改菜单项文字颜�?
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

        val isDisplaySplashAnim = Config.isDisplaySplashAnim
        val isDisplayGuide = Config.isDisplayGuide
        val isDisplayHomeSkeletonAnim = Config.isDisplayHomeSkeletonAnim

        binding.apply {
            radioStartAnim.isChecked = isDisplaySplashAnim
            radioStartAnim.onClick {
                val check = storage.getWithAnnotation<Boolean>(PreferKey.SPLASH_ANIM)
                radioStartAnim.isChecked = !check
                Config.isDisplaySplashAnim = !check
            }

            radioGuideShow.isChecked = isDisplayGuide
            radioGuideShow.onClick {
                val check = storage.getWithAnnotation<Boolean>(PreferKey.GUIDE_PAGE)
                radioGuideShow.isChecked = !check
                Config.isDisplayGuide = !check
            }

            radioHomeSkeletonAnim.isChecked = isDisplayHomeSkeletonAnim
            radioHomeSkeletonAnim.onClick {
                val check = storage.getWithAnnotation<Boolean>(PreferKey.HOME_SKELETON_ANIM)
                radioHomeSkeletonAnim.isChecked = !check
                Config.isDisplayHomeSkeletonAnim = !check
            }

            crumble.onClick {
                showDialogFragment<CrashLogsDialog>()
            }
        }
    }
}