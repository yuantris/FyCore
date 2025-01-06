package com.core.fy.android.room

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.core.fy.android.room.dao.FunctionDao
import com.core.fy.android.room.dao.UserDao
import com.core.fy.android.room.entity.Function
import com.core.fy.android.room.entity.User
import com.core.libraries.Android

@Database(entities = [User::class, Function::class],
    version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun functionDao(): FunctionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    Android.context,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
