package io.core.common.base.component.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import io.core.common.CoreConfig
import io.core.common.util.extensions.ui.restart
import io.core.common.util.extensions.ui.startActivity


/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/11 8:57
 * @description
 * @author Yuan
 */
class RestartActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity<RestartActivity>()
        }

        fun restart(context: Context) {
            CoreConfig.Crash.afterJumpActivity?.let {
                val intent = Intent(context, it)
                if (context !is Activity) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                context.startActivity(intent)
            } ?: run {
                context.restart()
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        super.onCreate(savedInstanceState)

        restart(this)
        finish()
    }
}