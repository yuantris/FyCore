package com.core.fy.android.function

import android.os.Bundle
import androidx.activity.viewModels
import com.core.fy.android.databinding.ActivityRoomBinding
import com.core.fy.android.room.AppDatabase
import com.core.fy.android.room.UserVMFactory
import com.core.fy.android.room.entity.User
import com.core.fy.android.room.repository.UserRepository
import com.core.fy.android.viewmodel.UserVM
import com.core.libraries.base.activity.ReflectBindingActivity
import com.core.libraries.base.ext.launchSafe
import com.core.libraries.base.ext.launchSafeAsync
import com.core.libraries.base.ext.logD
import com.core.libraries.base.ext.logE
import com.core.libraries.base.ext.logI
import com.core.libraries.base.ext.logW
import com.core.libraries.enums.ViewStatus
import com.core.libraries.util.CoreUtil
import com.core.libraries.util.ToastUtil
import com.core.libraries.view.TitleBar

class RoomActivity : ReflectBindingActivity<ActivityRoomBinding>() {

    private val userVM by viewModels<UserVM> {
        UserVMFactory(UserRepository.singletonCreate())
    }

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)

        launchSafe {
            userVM.getUserById(1).collect {
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

        // userVM.getAllUsers()
        // userVM.deleteAll()
    }

    override fun setListener() {
        super.setListener()
        mBinding.apply {
            titleBar.setOnTitleClickListener(object : TitleBar.OnTitleClickListener {
                override fun onBackClick() {
                    finish()
                }
            })

            add.setOnClickListener {
                launchSafeAsync {
                    val user = User(name = name.text.toString(), age = age.text.toString().toInt())
                    val insert = userVM.insert(user)
                    "insert: ${insert.isActive}".logW()
                    ToastUtil.show("插入成功")
                }
            }

            del.setOnClickListener {
                launchSafeAsync {
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
                launchSafeAsync {
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

                    launchSafeAsync {
                        bean.age = 19
                        AppDatabase.getDatabase().userDao().update(bean)
                        val byNameNext = AppDatabase.getDatabase().userDao().getUserById(bean.id)
                        "根据名字查询到用户：${byNameNext?.name}，年龄：${byNameNext?.age}".logI()
                    }
                    mBinding.dataShow.text = bean.age.toString()
                }
            } else {
                mBinding.dataShow.text = "暂无数据"
            }
        }

        userVM.viewStatus.observe(this) {
            when (it) {
                ViewStatus.SUCCESS -> {}
            }


        }
    }

}