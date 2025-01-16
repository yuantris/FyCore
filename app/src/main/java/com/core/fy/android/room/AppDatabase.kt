package com.core.fy.android.room

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.core.fy.android.room.dao.FunctionDao
import com.core.fy.android.room.dao.UserDao
import com.core.fy.android.room.entity.Function
import com.core.fy.android.room.entity.User
import io.core.Android
import io.core.common.util.ext.appCtx

val appDb by lazy {
    Room.databaseBuilder(appCtx, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
        .fallbackToDestructiveMigration() //如果数据库升级失败了，删除重新创建
        .enableMultiInstanceInvalidation() //多进程查询支持
        .build()
}

@Database(
    entities = [User::class, Function::class],
    version = 1, exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun functionDao(): FunctionDao

    companion object {
        const val DATABASE_NAME = "app_database.db"
    }
}
