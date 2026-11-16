package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao

interface TransactionDao {
    @Query(
        """
        SELECT * FROM transactions
        WHERE dateEpochDay BETWEEN :fromDay AND :toDay
          AND (:category IS NULL OR category = :category)
        ORDER BY dateEpochDay DESC, id DESC
        """
    )
    fun observe(fromDay: Long, toDay: Long, category: String?): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)
}