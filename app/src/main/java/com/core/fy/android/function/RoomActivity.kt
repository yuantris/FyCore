package com.core.fy.android.function

import android.os.Bundle
import androidx.activity.viewModels
import com.core.fy.android.databinding.ActivityRoomBinding
import com.core.fy.android.room.AppDatabase
import com.core.fy.android.room.VMFactory
import com.core.fy.android.room.entity.User
import com.core.fy.android.room.repository.UserRepository
import com.core.fy.android.viewmodel.UserVM
import com.core.libraries.base.activity.ReflectBindingActivity
import com.core.libraries.base.ext.launchAsync
import com.core.libraries.base.ext.launchSync
import com.core.libraries.base.ext.logD
import com.core.libraries.base.ext.logE
import com.core.libraries.base.ext.logI
import com.core.libraries.base.ext.toast
import com.core.libraries.base.vm.ViewStatus
import com.core.libraries.util.CoreUtil
import com.core.libraries.util.ToastUtil

class RoomActivity : ReflectBindingActivity<ActivityRoomBinding>() {

    private val userVM by viewModels<UserVM> {
        VMFactory(UserRepository.singletonCreate())
    }

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)

        launchSync {
            userVM.getUserAsync(1).await().let {
                "查询到：${it?.name}，details：${it?.age}".logD()
//                if (it != null) {
//                    it.age = 10085
//                    userVM.update(it)
//                } else {
//                    "查询为空".logE()
//                    val origin = User(name = "fy", age = 16)
//                    userVM.insert(origin)
//                    "插入年龄为${origin.age}岁的用户".logI()
//                }

                // 先判断是否为空，如果不为空则更新，否则插入
                it?.let { user ->
                    user.name = CoreUtil.File.generateNameNoExtension("name")
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
                launchAsync {
                    val user = User(name = name.text.toString(), age = age.text.toString().toInt())
                    val insert = userVM.insert(user)
                    ToastUtil.show("插入成功")
                }
            }

            del.setOnClickListener {
                launchAsync {
                    userVM.getAll().collect {
                        if (it.isNotEmpty()) {
                            val user = it.last()
                            userVM.delete(user)
                            ToastUtil.show("删除成功")
                        }
                    }
                }
            }

            update.setOnClickListener {
                launchAsync {
                    userVM.getAll().collect {
                        if (it.isNotEmpty()) {
                            val user = it.last()
                            user.name = name.text.toString()
                            user.age = age.text.toString().toInt()
                            userVM.update(user)
                            ToastUtil.show("更新成功")
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

                    launchAsync {
                        bean.age = 19
                        AppDatabase.getDatabase().userDao().update(bean)
                        val byNameNext = userVM.getUserAsync(bean.id).await()
                        "根据名字查询到用户：${byNameNext?.name}，年龄：${byNameNext?.age}".logI()
                    }
                    binding.dataShow.text = bean.age.toString()
                }
            } else {
                binding.dataShow.text = "暂无数据"
            }
        }

        userVM.status.observe(this) { status ->
            when (status) {
                ViewStatus.SUCCESS -> toast("成功")
                ViewStatus.ERROR -> toast("失败")
                else -> "其他状态".logD()
            }
        }
    }

}