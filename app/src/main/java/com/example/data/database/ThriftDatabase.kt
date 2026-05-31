package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.models.Product
import com.example.data.models.ReturnItem
import com.example.data.models.LossRecord
import com.example.data.models.Order

@Database(entities = [Product::class, ReturnItem::class, LossRecord::class, Order::class], version = 3, exportSchema = false)
abstract class ThriftDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun returnDao(): ReturnDao
    abstract fun lossDao(): LossDao
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: ThriftDatabase? = null

        fun getDatabase(context: Context): ThriftDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ThriftDatabase::class.java,
                    "thrift_hive_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
