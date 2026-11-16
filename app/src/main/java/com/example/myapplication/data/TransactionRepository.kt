package com.example.myapplication.data

import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeTransactions(
        fromDay: Long,
        toDay: Long,
        category: Category? = null
    ): Flow<List<TransactionEntity>>

    suspend fun getTransaction(id: Long): TransactionEntity?
    suspend fun addTransaction(transaction: TransactionEntity)
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
}