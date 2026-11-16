package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import com.example.myapplication.data.TransactionRepository
import java.util.Calendar
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Uses the ONE shared TransactionRepository owned by the team leader.
 *
 * The repository interface on main is currently EMPTY, so the app's composition
 * root must supply its read/delete operations once the agreed contract exists.
 * These functions are adapters to the SHARED repository, not another database.
 * Do NOT edit shared data models or introduce a separate transaction store here.
 *
 * The adapter should map each shared transaction to TransactionListItem and
 * expose a Flow that re-emits whenever transactions are added/edited/deleted.
 */
class TransactionsViewModel(
    private val repository: TransactionRepository,
    private val observeHistory: (TransactionRepository) -> Flow<List<TransactionListItem>>,
    private val deleteFromHistory: suspend (TransactionRepository, String) -> Unit,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    private fun mutateState(transform: (TransactionsUiState) -> TransactionsUiState) {
        _uiState.value = transform(_uiState.value)
    }

    private var allTransactions: List<TransactionListItem> = emptyList()
    private var observationJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        observationJob?.cancel()
        mutateState { it.copy(isLoading = true, errorMessage = null) }
        observationJob = scope.launch {
            try {
                observeHistory(repository)
                    .catch { throwable ->
                        mutateState {
                            it.copy(
                                isLoading = false,
                                errorMessage = throwable.message ?: "Unable to load transactions."
                            )
                        }
                    }
                    .collect { transactions ->
                        allTransactions = transactions
                        updateVisibleTransactions()
                        mutateState { it.copy(isLoading = false, errorMessage = null) }
                    }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                // In particular, handle a synchronous exception when creating the Flow.
                mutateState {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Unable to load transactions."
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        mutateState { it.copy(searchQuery = query) }
        updateVisibleTransactions()
    }

    fun onTypeFilterChanged(type: TransactionTypeFilter) {
        mutateState { it.copy(selectedType = type) }
        updateVisibleTransactions()
    }

    fun onCategoryFilterChanged(category: String?) {
        mutateState { it.copy(selectedCategory = category) }
        updateVisibleTransactions()
    }

    fun onDateFilterChanged(filter: TransactionDateFilter) {
        mutateState { it.copy(selectedDateFilter = filter) }
        updateVisibleTransactions()
    }

    fun clearActionError() {
        mutateState { it.copy(actionErrorMessage = null) }
    }

    /** Only call this after the UI has shown and accepted its confirmation dialog. */
    fun deleteConfirmed(id: String) {
        if (_uiState.value.isDeleting) return
        if (allTransactions.none { it.id == id }) return
        scope.launch {
            mutateState { it.copy(isDeleting = true, actionErrorMessage = null) }
            try {
                deleteFromHistory(repository, id)
                // Immediate, local UI feedback even if the Repository emits later.
                allTransactions = allTransactions.filterNot { it.id == id }
                updateVisibleTransactions()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutateState {
                    it.copy(
                        actionErrorMessage = exception.message
                            ?: "Couldn't delete the transaction. Please try again."
                    )
                }
            } finally {
                mutateState { it.copy(isDeleting = false) }
            }
        }
    }

    private fun updateVisibleTransactions() {
        val state = _uiState.value
        val query = state.searchQuery.trim()
        val earliestAllowed = firstAllowedDate(state.selectedDateFilter)
        val filtered = allTransactions.asSequence()
            .filter { item ->
                query.isBlank() || item.description.contains(query, ignoreCase = true) ||
                    item.note.contains(query, ignoreCase = true)
            }
            .filter { item ->
                when (state.selectedType) {
                    TransactionTypeFilter.ALL -> true
                    TransactionTypeFilter.INCOME -> item.isIncome
                    TransactionTypeFilter.EXPENSE -> !item.isIncome
                }
            }
            .filter { item ->
                state.selectedCategory == null ||
                    item.category.equals(state.selectedCategory, ignoreCase = true)
            }
            .filter { item -> earliestAllowed == null || item.dateMillis >= earliestAllowed }
            .sortedByDescending { it.dateMillis }
            .toList()

        mutateState {
            it.copy(
                visibleTransactions = filtered,
                availableCategories = allTransactions.map { transaction -> transaction.category }
                    .filter(String::isNotBlank).distinct().sorted(),
                totalIncome = filtered.filter { transaction -> transaction.isIncome }
                    .sumOf { transaction -> abs(transaction.amount) },
                totalExpenses = filtered.filterNot { transaction -> transaction.isIncome }
                    .sumOf { transaction -> abs(transaction.amount) }
            )
        }
    }

    private fun firstAllowedDate(filter: TransactionDateFilter): Long? {
        if (filter == TransactionDateFilter.ALL) return null
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        when (filter) {
            TransactionDateFilter.LAST_7_DAYS -> calendar.add(Calendar.DAY_OF_YEAR, -6)
            TransactionDateFilter.LAST_30_DAYS -> calendar.add(Calendar.DAY_OF_YEAR, -29)
            TransactionDateFilter.THIS_MONTH -> calendar.set(Calendar.DAY_OF_MONTH, 1)
            TransactionDateFilter.ALL -> Unit
        }
        return calendar.timeInMillis
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}
