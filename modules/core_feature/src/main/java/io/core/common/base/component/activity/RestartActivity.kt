package io.core.common.base.component.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.core.Android.homeActivity
import kotlin.system.exitProcess


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
            val intent = Intent(context, RestartActivity::class.java)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        fun restart(context: Context) {
            homeActivity?.let {
                val intent = Intent(context, homeActivity)
                if (context !is Activity) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } ?: run {
                exitProcess(0)
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restart(this)
        finish()
    }
}