package com.example.myapplication.viewmodel // Check your actual package name

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// NOTE: You must import your actual Transaction and Repository classes
// from the 'data' folder. Check their exact names.
import com.example.smartexpensetracker.data.TransactionRepository

// This holds the state for the UI
data class AnalysisUiState(
    val totalSpending: Double = 0.0,
    val categorySummary: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AnalysisViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val _uiState = MutableLiveData<AnalysisUiState>()
    val uiState: LiveData<AnalysisUiState> get() = _uiState

    fun loadData() {
        _uiState.value = AnalysisUiState(isLoading = true)

        viewModelScope.launch {
            try {
                // 1. Get data from shared repository
                // (Replace 'getAllTransactions()' with the actual function name in your repo)
                val transactions = repository.getAllTransactions()

                if (transactions.isNullOrEmpty()) {
                    // 2. Handle Empty Data
                    _uiState.value = AnalysisUiState(totalSpending = 0.0)
                    return@launch
                }

                // 3. Calculate Total Spending (Assuming type is an enum/string "EXPENSE")
                val total = transactions
                    .filter { it.type == "EXPENSE" } // Adjust based on your TransactionType
                    .sumOf { it.amount }

                // 4. Summarize by Category
                val summary = transactions
                    .filter { it.type == "EXPENSE" }
                    .groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }

                _uiState.value = AnalysisUiState(
                    totalSpending = total,
                    categorySummary = summary,
                    isLoading = false
                )

            } catch (e: Exception) {
                // 5. Handle Calculation Errors
                _uiState.value = AnalysisUiState(errorMessage = "Error: ${e.message}")
            }
        }
    }
}