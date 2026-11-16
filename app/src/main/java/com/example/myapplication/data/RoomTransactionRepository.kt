package com.example.myapplication.data

import kotlinx.coroutines.flow.Flow

class RoomTransactionRepository (private val dao: TransactionDao) : TransactionRepository{
    override fun observeTransactions(
        fromDay: Long,
        toDay: Long,
        category: Category?
    ): Flow<List<TransactionEntity>> = dao.observe(fromDay, toDay, category?.name)

    override suspend fun getTransaction(id: Long): TransactionEntity? = dao.getById(id)

    override suspend fun addTransaction(transaction: TransactionEntity) {
        dao.insert(transaction)
    }

    override suspend fun updateTransaction(transaction: TransactionEntity) {
        dao.update(transaction)
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        dao.delete(transaction)
    }
}