package com.core.fy.android.function.camerax

import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import com.core.fy.android.databinding.ActivityCameraxBinding
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.util.ext.ui.onClick

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/2/5 15:52
 * @description
 * @author Yuan
 */
class CameraXActivity : ReflectBindingActivity<ActivityCameraxBinding>() {

    private var helper: CameraHelper? = null

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        helper = CameraHelper.Builder(this, this, binding.preview)
            .enableImageCapture(true)
            .setLensFacing(CameraSelector.LENS_FACING_FRONT)
            .setImageResolution(Size(1920, 1080))
            .setErrorCallback { error ->
                Log.e("CameraHelper", error)
            }
            .build()
        XXPermissions.with(this)
            .permission(Permission.CAMERA)
            .request { _, _ ->
                helper?.initializeCamera()
                helper?.setZoomRatio(0.5f)
            }
    }

    override fun setListener() {
        binding.capture.onClick {
            //helper?.switchCamera()
            helper?.takePicture { bitmap, exception ->
                bitmap?.let {
                    binding.image.setImageBitmap(it)
                }
            }
        }

        binding.change.onClick {
            helper?.switchCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        helper?.shutdown()
    }
}