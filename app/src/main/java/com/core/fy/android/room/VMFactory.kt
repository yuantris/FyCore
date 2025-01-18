package com.core.fy.android.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.core.fy.android.room.repository.FunctionRepository
import com.core.fy.android.room.repository.UserRepository
import com.core.fy.android.viewmodel.FunctionVM
import com.core.fy.android.viewmodel.UserVM
import io.core.common.base.room.BaseRepository

@Suppress("UNCHECKED_CAST")
class VMFactory(private val repository: BaseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        when (modelClass) {
            UserVM::class.java -> return UserVM(repository as UserRepository) as T
            FunctionVM::class.java -> return FunctionVM(repository as FunctionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
