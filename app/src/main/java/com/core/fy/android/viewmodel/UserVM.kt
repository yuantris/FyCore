package com.core.fy.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.core.fy.android.room.entity.User
import com.core.fy.android.room.repository.UserRepository
import com.core.libraries.common.util.ext.tool.logD
import com.core.libraries.common.util.ext.tool.logE
import com.core.libraries.common.base.vm.BaseViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UserVM(private var repository: UserRepository) : BaseViewModel() {
    private val _userLiveData = MutableLiveData<List<User>>()
    val userLiveData: LiveData<List<User>>
        get() = _userLiveData

    fun insert(user: User) {
        launch({
            repository.dao.insert(user)
        })
    }

    fun delete(user: User) {
        launch({
            repository.dao.delete(user)
        })
    }

    fun update(user: User) {
        launch({
            repository.dao.update(user)
        })
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

    fun getUserById(id: Long, onSuccess: (User?) -> Unit) {
        flowLaunch(
            flowBlock = {
                flow {
                    emit(repository.getUserById(id))
                }
            },
            onSuccess = onSuccess,
            onError = { error ->
                "获取用户失败：${error.message}".logE()
            }
        )
    }


    fun getUserAsync(id: Long) = async {
        repository.getUserById(id)
    }

    fun getAllUsers() {
        launch(
            block = {
                val result = repository.queryAll()
                "查询所有：${result.size}".logD()
                result
            },
            onError = {
                "查询所有失败：${it.message}".logE()
            },
            onSuccess = {
                _userLiveData.postValue(it)
            }
        )
    }

    fun getAll(): Flow<List<User>> {
        return flow {
            val result = repository.queryAll()
            emit(result)
        }
    }


    fun deleteAll() = launch(
        block = {
            repository.deleteAll()
            val result = repository.queryAll()
            "删除后剩余：${result.size}".logD()
        },
        onError = {
            "删除所有失败：${it.message}".logE()
        }
    )
}