package com.core.fy.android

import android.content.Intent
import android.os.Bundle
import com.core.fy.android.databinding.ActivityMainBinding
import com.core.fy.android.function.DialogActivity
import com.core.fy.android.function.KeyboardActivity
import com.core.fy.android.function.RoomActivity
import com.core.libraries.base.activity.ReflectBindingActivity
import com.core.libraries.util.CoreUtil
import com.core.libraries.util.ToastUtil

class MainActivity : ReflectBindingActivity<ActivityMainBinding>() {
    private val TAG by lazy { "MainActivity_" }

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        val filename = CoreUtil.File.generateName("mp3")
        ToastUtil.show("filename: $filename")
    }

    override fun setListener() {
        super.setListener()
        mBinding.apply {
            keyboard.setOnClickListener {
                startActivity(Intent(this@MainActivity, KeyboardActivity::class.java))
            }
            room.setOnClickListener {
                startActivity(Intent(this@MainActivity, RoomActivity::class.java))
            }
            dialog.setOnClickListener {
                startActivity(Intent(this@MainActivity, DialogActivity::class.java))
            }
        }
    }

}