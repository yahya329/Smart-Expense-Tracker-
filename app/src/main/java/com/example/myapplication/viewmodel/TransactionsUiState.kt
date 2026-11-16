package com.example.myapplication.viewmodel

/** Presentation-only data. Map the shared TransactionEntity to this in the app wiring. */
data class TransactionListItem(
    val id: String,
    val amount: Double,
    val isIncome: Boolean,
    val category: String,
    val dateMillis: Long,
    val description: String = "",
    val note: String = ""
)

enum class TransactionTypeFilter { ALL, INCOME, EXPENSE }
enum class TransactionDateFilter { ALL, LAST_7_DAYS, LAST_30_DAYS, THIS_MONTH }

data class TransactionsUiState(
    val visibleTransactions: List<TransactionListItem> = emptyList(),
    val availableCategories: List<String> = emptyList(),
    val searchQuery: String = "",
    val selectedType: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val selectedCategory: String? = null,
    val selectedDateFilter: TransactionDateFilter = TransactionDateFilter.ALL,
    /** The totals reflect all currently active search/filter selections. */
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val actionErrorMessage: String? = null
)
