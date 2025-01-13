package com.core.fy.android.room

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.core.fy.android.room.dao.FunctionDao
import com.core.fy.android.room.dao.UserDao
import com.core.fy.android.room.entity.Function
import com.core.fy.android.room.entity.User
import com.core.libraries.Android

@Database(
    entities = [User::class, Function::class],
    version = 1, exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun functionDao(): FunctionDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    Android.context,
                    AppDatabase::class.java,
                    "app_database"
                ).fallbackToDestructiveMigration() //如果数据库升级失败了，删除重新创建
                    .enableMultiInstanceInvalidation() //多进程查询支持
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
