package com.example.myapplication.data

import androidx.room.Database
import androidx.room.RoomDatabase

import java.time.YearMonth

data class DayRange(val fromDay: Long, val toDay: Long)



@Database(entities = [TransactionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}

