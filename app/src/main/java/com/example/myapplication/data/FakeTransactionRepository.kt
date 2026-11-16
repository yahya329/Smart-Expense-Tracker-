package com.example.myapplication.data

import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeTransactionRepository : TransactionRepository {

    private val items = MutableStateFlow(sampleData())
    private var nextId = 100L

    override fun observeTransactions(
        fromDay: Long,
        toDay: Long,
        category: Category?
    ): Flow<List<TransactionEntity>> = items.map { list ->
        list.filter { it.dateEpochDay in fromDay..toDay && (category == null || it.category == category) }
            .sortedWith(compareByDescending<TransactionEntity> { it.dateEpochDay }.thenByDescending { it.id })
    }

    override suspend fun getTransaction(id: Long): TransactionEntity? =
        items.value.firstOrNull { it.id == id }

    override suspend fun addTransaction(transaction: TransactionEntity) {
        val newId = nextId++
        items.update { it + transaction.copy(id = newId) }
    }

    override suspend fun updateTransaction(transaction: TransactionEntity) {
        items.update { list -> list.map { if (it.id == transaction.id) transaction else it } }
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        items.update { list -> list.filterNot { it.id == transaction.id } }
    }

    private fun sampleData(): List<TransactionEntity> {
        val start = YearMonth.now().atDay(1).toEpochDay()// modify this error here
        return listOf(
            TransactionEntity(1, 400_000, TransactionType.INCOME, Category.SALARY, "Salary", start),
            TransactionEntity(2, 90_000, TransactionType.EXPENSE, Category.BILLS, "Rent share", start + 1),
            TransactionEntity(3, 12_000, TransactionType.EXPENSE, Category.TRANSPORT, "Metro card", start + 2),
            TransactionEntity(4, 45_000, TransactionType.EXPENSE, Category.FOOD, "Groceries", start + 3),
            TransactionEntity(5, 40_000, TransactionType.EXPENSE, Category.SHOPPING, "Shoes", start + 4),
            TransactionEntity(6, 14_500, TransactionType.EXPENSE, Category.TRANSPORT, "Taxi", start + 5),
            TransactionEntity(7, 8_500, TransactionType.EXPENSE, Category.FOOD, "Lunch", start + 5)
        )
    }
}