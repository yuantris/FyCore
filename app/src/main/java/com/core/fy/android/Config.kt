package com.core.fy.android

import com.core.fy.android.constants.PreferKey
import io.core.base.appCtx
import io.core.base.CoreConfig
import io.core.engine.storage.getWithAnnotation
import io.core.engine.storage.put
import io.core.engine.storage.storage

/**
 * ██�? ██╗███████╗██╗   ██�?   ┌──────────�?
 * ╚██╗██╔╝██╔════╝╚██╗ ██╔╝    �?加载进度 │▰▰▰▰▰▰▰▰◯ 87%
 *  ╚███╔╝ █████╗   ╚████╔�?    └──────────�?
 *  ██╔██╗ ██╔══╝    ╚██╔�?     ╱╲▲△△△△△△△�?
 * ██╔╝ ██╗██╗        ██�?      �?▏正在渲染配置矩�?..
 * ╚═�? ╚═╝╚═╝        ╚═�?      ╲╱▼▽▽▽▽▽▽▽�?
 * 注释的艺术，正在生成......
 * 模块加载阶段 ████████████ 100%
 * 最后编译阶�?████████░░░░ 65% (�?F12 解锁彩蛋)
 *
 * @Author [Yuan]
 * 2025/3/13 14:49
 */
object Config {

    // 是否显示启动动画
    var isDisplaySplashAnim
        get() = storage.getWithAnnotation<Boolean>(PreferKey.SPLASH_ANIM)
        set(value) {
            storage.put(PreferKey.SPLASH_ANIM, value)
        }
    var isDisplayGuide
        get() = storage.getWithAnnotation<Boolean>(PreferKey.GUIDE_PAGE)
        set(value) {
            storage.put(PreferKey.GUIDE_PAGE, value)
        }
    var isDisplayHomeSkeletonAnim
        get() = storage.getWithAnnotation<Boolean>(PreferKey.HOME_SKELETON_ANIM)
        set(value) {
            storage.put(PreferKey.HOME_SKELETON_ANIM, value)
        }

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