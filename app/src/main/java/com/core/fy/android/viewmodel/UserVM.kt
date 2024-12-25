package com.core.fy.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.core.fy.android.room.dao.UserDao
import com.core.fy.android.room.entity.User
import com.core.fy.android.room.repository.UserRepository
import com.core.libraries.base.ext.logD
import com.core.libraries.base.ext.logE
import com.core.libraries.base.room.RoomRepository
import com.core.libraries.base.vm.BaseViewModel

class UserVM(private var repository: RoomRepository<User>) : BaseViewModel() {
    private val _userLiveData = MutableLiveData<List<User>>()
    val userLiveData: LiveData<List<User>>
        get() = _userLiveData

    fun insert(user: User) = launch(
        {
            val result = repository.insert(user)
        }, {
            "插入失败：${it.message}".logE()
        }
    )

    fun delete(user: User) = launch(
        {
            val result = repository.delete(user)
        }, {
            "删除失败：${it.message}".logE()
        }
    )

    fun update(user: User) = launch(
        {
            val result = repository.update(user)
        }, {
            "更新失败：${it.message}".logE()
        }
    )

    fun getUserByName(name: String) = launch(
        {
            val result = (repository as UserRepository).getUserByName(name)
            val users = mutableListOf(result)
            _userLiveData.postValue(users)
        }, {
            "查询失败：${it.message}".logE()
        }
    )


    fun getAllUsers() = launch(
        {
            val result = repository.queryAll()
            "查询所有：${result.size}".logD()
            _userLiveData.postValue(result)
        }, {
            "查询所有失败：${it.message}".logE()
        }
    )


    fun deleteAll() = launch(
        {
            (repository as UserRepository).deleteAll()
            val result = repository.queryAll()
            "删除后剩余：${result.size}".logD()
        }, {
            "删除所有失败：${it.message}".logE()
        }
    )

    fun getTableName() = repository.dao.getTableName()
}