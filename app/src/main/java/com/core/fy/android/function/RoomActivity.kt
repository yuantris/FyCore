package com.core.fy.android.function

import android.os.Bundle
import android.view.Gravity
import androidx.activity.viewModels
import com.core.fy.android.databinding.ActivityRoomBinding
import com.core.fy.android.room.AppDatabase
import com.core.fy.android.room.UserVMFactory
import com.core.fy.android.room.entity.User
import com.core.fy.android.room.repository.UserRepository
import com.core.fy.android.viewmodel.UserVM
import com.core.libraries.base.activity.ReflectBindingActivity
import com.core.libraries.base.ext.logE

class RoomActivity : ReflectBindingActivity<ActivityRoomBinding>() {

    private val userVM by viewModels<UserVM> {
        UserVMFactory(UserRepository.getInstance())
    }


    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        userVM.insert(User(name = "fy", age = 18))
        //userVM.getUserByName("fy")
        userVM.getAllUsers()
        userVM.deleteAll()

        userVM.getTableName().logE()
//
//        lifecycleScope.launch {
//            userDao.insert(User(name = "fy", age = 18))
//            val user = userDao.getUserByName("fy")
//            mBinding.dataShow.text = user.age.toString()
//
//        }
    }

    override fun setListener() {
        super.setListener()
        mBinding.apply {
            titleBar.setLeftButton(null) {
                finish()
            }
        }
    }

    override fun observers() {
        super.observers()
        userVM.userLiveData.observe(this) {
            if (it.isNotEmpty()){
                mBinding.dataShow.text = it[0].age.toString()
                mBinding.dataShowAll.text = it.size.toString()
            }

        }
    }

}