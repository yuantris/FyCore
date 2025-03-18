package com.core.fy.android

import com.core.fy.android.constants.PreferKey
import io.core.appCtx
import io.core.common.CoreConfig
import io.core.common.CoreConfig.Alert.negativeColor
import io.core.common.CoreConfig.Alert.positiveColor
import io.core.engine.storage.getWithAnnotation
import io.core.engine.storage.storage

/**
 * ██╗  ██╗███████╗██╗   ██╗    ┌──────────┐
 * ╚██╗██╔╝██╔════╝╚██╗ ██╔╝    │ 加载进度 │▰▰▰▰▰▰▰▰◯ 87%
 *  ╚███╔╝ █████╗   ╚████╔╝     └──────────┘
 *  ██╔██╗ ██╔══╝    ╚██╔╝      ╱╲▲△△△△△△△△
 * ██╔╝ ██╗██╗        ██║       ▉ ▏正在渲染配置矩阵...
 * ╚═╝  ╚═╝╚═╝        ╚═╝       ╲╱▼▽▽▽▽▽▽▽▽
 * 注释的艺术，正在生成......
 * 模块加载阶段 ████████████ 100%
 * 最后编译阶段 ████████░░░░ 65% (按 F12 解锁彩蛋)
 *
 * @Author [Yuan]
 * 2025/3/13 14:49
 */
object Config {
    // 是否显示启动动画
    var isDisplaySplashAnim = storage.getWithAnnotation<Boolean>(PreferKey.SPLASH_ANIM)
    var isDisplayGuide = storage.getWithAnnotation<Boolean>(PreferKey.GUIDE_PAGE)
    var isDisplayHomeSkeletonAnim = storage.getWithAnnotation<Boolean>(PreferKey.HOME_SKELETON_ANIM)

    init {
        CoreConfig.configure {
            crash {
                allowMultiProcess = true
                afterJumpActivity = MainActivity::class.java
            }
            alert {
                positiveColor = appCtx.getColor(R.color.md_indigo_500)
                negativeColor = appCtx.getColor(R.color.md_red_300)
            }
        }
    }
}