package com.core.fy.android.function

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.blankj.utilcode.util.ActivityUtils
import com.core.fy.android.databinding.ActivityTestPageBinding
import com.core.fy.android.main.fragment.code.JavaFragment
import com.core.fy.android.main.fragment.code.KotlinFragment
import com.google.android.material.tabs.TabLayoutMediator
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.helper.pool.ObjectPoolBuilder
import io.core.common.helper.track.AppLifecycleTracker
import io.core.common.util.Preferences
import io.core.common.util.Toaster
import io.core.common.util.extensions.logD
import io.core.common.util.extensions.logW
import io.core.other.IntentData
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
 * 2025/1/13 16:43
 * @description
 * @author Yuan
 */
class TestPageActivity : ReflectBindingActivity<ActivityTestPageBinding>() {

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        ActivityUtils.getTopActivity()?.let {
            "topActivity1: ${it.javaClass.simpleName}".logD()
        }
        AppLifecycleTracker.getTopActivity()?.let {
            "topActivity2: ${it.javaClass.simpleName}".logD()
        }

        // 初始化 ViewPager2
        val viewPager = binding.vp.apply {
            adapter = TabPagerAdapter(this@TestPageActivity)
            offscreenPageLimit = 1
        }
        // 绑定 TabLayout 和 ViewPager2
        TabLayoutMediator(binding.tab, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Kotlin"
                1 -> "Java"
                else -> null
            }
        }.attach()


        val paintPool = ObjectPoolBuilder.android(
            maxSize = 8,
            create = { "1" }
        ) {
            destroy = {

            }
        }

        lifecycleScope.launch {
            val paint = paintPool.acquire()
            Toaster.show(paint)
            paintPool.release("2")
            val acquire = paintPool.acquire()
            Toaster.show(acquire)
        }

        Preferences.putValue("test", setOf(1, 2, 3))
        val test = Preferences.getValue("test", setOf(0))
        test.logD()
        IntentData.put("test", null)
        val any = IntentData.get<Any>("test")
        any?: "null".logW()
    }

    inner class TabPagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> KotlinFragment()
            1 -> JavaFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}