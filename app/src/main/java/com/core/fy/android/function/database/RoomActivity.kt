package com.core.fy.android.function.database

import android.os.Bundle
import androidx.activity.viewModels
import com.core.fy.android.databinding.ActivityRoomBinding
import com.core.fy.android.room.VMFactory
import com.core.fy.android.room.appDb
import com.core.fy.android.room.entity.User
import com.core.fy.android.room.repository.UserRepository
import com.core.fy.android.viewmodel.UserVM
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.base.vm.ViewStatus
import io.core.common.util.CoreUtil
import io.core.common.util.Toaster
import io.core.common.util.extensions.cool.launch
import io.core.common.util.extensions.logD
import io.core.common.util.extensions.logE
import io.core.common.util.extensions.logI
import io.core.common.util.extensions.logW
import io.core.common.util.extensions.ui.toast

class RoomActivity : ReflectBindingActivity<ActivityRoomBinding>() {

    private val userVM by viewModels<UserVM> {
        VMFactory(UserRepository)
    }

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)

        userVM.getUserById(1) {
            "查询到：${it?.name}，details：${it?.age}".logW()
        }

        launch {
            userVM.getUserAsync(1).let {
                "查询到：${it?.name}，details：${it?.age}".logD()

                // 先判断是否为空，如果不为空则更新，否则插入
                it?.let { user ->
                    user.name = CoreUtil.Files.generateNameNoExtension("name")
                    user.age = 100
                    userVM.update(user)
                    "查询到：${user.name}，details已更新为：${user.age}".logD()
                } ?: run {
                    "查询为空".logE()
                    val origin = User(name = "fy", age = 16)
                    userVM.insert(origin)
                    "插入年龄为${origin.age}岁的用户".logI()
                }
            }
        }

    }

    override fun setListener() {
        super.setListener()
        binding.apply {
            add.setOnClickListener {
                launch {
                    val user = User(name = name.text.toString(), age = age.text.toString().toInt())
                    val insert = userVM.insert(user)
                    Toaster.show("插入成功")
                }
            }

            del.setOnClickListener {
                launch {
                    userVM.getAll().collect {
                        if (it.isNotEmpty()) {
                            val user = it.last()
                            userVM.delete(user)
                            Toaster.show("删除成功")
                        }
                    }
                }
            }

            update.setOnClickListener {
                launch {
                    userVM.getAll().collect {
                        if (it.isNotEmpty()) {
                            val user = it.last()
                            user.name = name.text.toString()
                            user.age = age.text.toString().toInt()
                            userVM.update(user)
                            Toaster.show("更新成功")
                        }
                    }
                }
            }
        }
    }

    override fun observers() {
        super.observers()
        userVM.userLiveData.observe(this) {
            if (it.isNotEmpty()) {
                it.forEach { bean ->
                    "查询到ID：${bean.id}，用户：${bean.name}，年龄：${bean.age}".logD()

                    launch {
                        bean.age = 19
                        appDb.userDao().update(bean)
                        val byNameNext = userVM.getUserAsync(bean.id)
                        "根据名字查询到用户：${byNameNext?.name}，年龄：${byNameNext?.age}".logI()
                    }
                    binding.dataShow.text = bean.age.toString()
                }
            } else {
                binding.dataShow.text = "暂无数据"
            }
        }

        userVM.viewState.observe(this) { status ->
            when (status) {
                ViewStatus.SUCCESS -> toast("成功")
                ViewStatus.ERROR -> toast("失败")
                else -> "其他状态".logD()
            }
        }
    }

}