package com.core.fy.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.core.fy.android.room.AppDatabase
import com.core.fy.android.room.dao.UserDao
import com.core.fy.android.room.entity.User
import com.core.fy.android.room.repository.UserRepository
import com.core.libraries.base.ext.launchAsync
import com.core.libraries.base.ext.logD
import com.core.libraries.base.ext.logE
import com.core.libraries.base.room.RoomRepository
import com.core.libraries.base.vm.BaseViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class UserVM(private var repository: UserRepository) : BaseViewModel() {
    private val _userLiveData = MutableLiveData<List<User>>()
    val userLiveData: LiveData<List<User>>
        get() = _userLiveData

    fun insert(user: User) = launchAsync {
        repository.insert(user)
    }

    fun delete(user: User) = launchAsync {
        repository.delete(user)
    }

    fun update(user: User) = launchAsync {
        repository.update(user)
    }

    fun getUserByName(name: String): User {
        val user = repository.dao.getUserSync(name)
        return user
    }

    fun getUser(name: String): Flow<User?> {
        return flow {
            // 使用挂起函数获取用户
            val result = repository.getUserByName(name)
            // 通过 emit 发送数据到观察者
            emit(result)
        }
    }

    fun getUserById(id: Long): Flow<User?> {
        return flow {
            val result = repository.getUserById(id)
            emit(result)
        }
    }

    fun getUserAsync(name: String): Deferred<User?> {
        return viewModelScope.async {
            repository.getUserByName(name)
        }
    }


    fun getAllUsers() = launch(
        {
            val result = repository.queryAll()
            "查询所有：${result.size}".logD()
            _userLiveData.postValue(result)
        }, {
            "查询所有失败：${it.message}".logE()
        }
    )

    fun getAll(): Flow<List<User>> {
        return flow {
            val result = repository.queryAll()
            emit(result)
        }
    }


    fun deleteAll() = launch(
        {
            repository.deleteAll()
            val result = repository.queryAll()
            "删除后剩余：${result.size}".logD()
        }, {
            "删除所有失败：${it.message}".logE()
        }
    )

    fun getTableName() = repository.dao.getTableName()
}