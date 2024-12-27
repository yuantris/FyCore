package com.core.fy.android

import android.content.Intent
import android.os.Bundle
import com.core.fy.android.databinding.ActivityMainBinding
import com.core.fy.android.function.CustomToastActivity
import com.core.fy.android.function.DialogActivity
import com.core.fy.android.function.KeyboardActivity
import com.core.fy.android.function.RoomActivity
import com.core.libraries.base.activity.ReflectBindingActivity
import com.core.libraries.base.ext.launchSafeAsync
import com.core.libraries.base.ext.startActivity
import com.core.libraries.base.ext.toast
import com.core.libraries.other.Toast
import com.core.libraries.util.CoreUtil

class MainActivity : ReflectBindingActivity<ActivityMainBinding>() {
    private val TAG by lazy { "MainActivity_" }

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        val filename = CoreUtil.File.generateNameNoExtension("mp3")
        //ToastUtil.show("filename: $filename")
        launchSafeAsync {
            toast("filename: $filename")
        }
    }

    override fun setListener() {
        super.setListener()
        mBinding.apply {
            keyboard.setOnClickListener {
                startActivity(KeyboardActivity::class.java)
            }
            room.setOnClickListener {
                startActivity(RoomActivity::class.java)
            }
            dialog.setOnClickListener {
                startActivity(DialogActivity::class.java)
            }
            toast.setOnClickListener {
                startActivity(CustomToastActivity::class.java)
            }
        }
    }

}