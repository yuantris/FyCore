package com.core.fy.android.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.core.fy.android.room.repository.UserRepository
import com.core.fy.android.viewmodel.UserVM
import com.core.libraries.base.room.BaseRepository

class UserVMFactory(private val repository: BaseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserVM::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserVM(repository as UserRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
