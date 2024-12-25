package com.core.fy.android.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.core.fy.android.room.repository.UserRepository
import com.core.fy.android.viewmodel.UserVM

class UserVMFactory(private val repository: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserVM::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserVM(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
