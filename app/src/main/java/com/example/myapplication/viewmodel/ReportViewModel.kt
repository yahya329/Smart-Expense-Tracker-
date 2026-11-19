package com.example.myapplication.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.TransactionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class ReportPeriod(val label: String) {
    LAST_7_DAYS("Last 7 Days"),
    THIS_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days"),
    ALL_TIME("All Time")
}

data class ReportUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
    val summary: ReportSummary? = null,
    val generatedReportText: String = "",
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false,
    val errorMessage: String? = null
)

class ReportViewModel(
    private val repository: TransactionRepository,
    private val reportBuilder: ReportBuilder = ReportBuilder()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        selectPeriod(ReportPeriod.THIS_MONTH)
    }

    fun selectPeriod(period: ReportPeriod) {
        _uiState.update {
            it.copy(
                selectedPeriod = period,
                isLoading = true,
                errorMessage = null
            )
        }

        val (fromDay, toDay) = calculateEpochRange(period)

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeTransactions(fromDay = fromDay, toDay = toDay)
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load transactions: ${throwable.message ?: "Unknown error"}"
                        )
                    }
                }
                .collect { transactions ->
                    try {
                        if (transactions.isEmpty()) {
                            _uiState.update {
                                it.copy(
                                    summary = null,
                                    generatedReportText = "",
                                    isLoading = false,
                                    isEmpty = true,
                                    errorMessage = null
                                )
                            }
                        } else {
                            val summary = reportBuilder.buildSummary(
                                transactions = transactions,
                                periodLabel = period.label,
                                fromEpochDay = fromDay,
                                toEpochDay = toDay
                            )
                            val readableReport = reportBuilder.buildReadableReport(summary)

                            _uiState.update {
                                it.copy(
                                    summary = summary,
                                    generatedReportText = readableReport,
                                    isLoading = false,
                                    isEmpty = false,
                                    errorMessage = null
                                )
                            }
                        }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Error generating report: ${e.message ?: "Unexpected error"}"
                            )
                        }
                    }
                }
        }
    }

    fun shareReport(context: Context) {
        val reportText = _uiState.value.generatedReportText
        if (reportText.isBlank()) {
            _uiState.update { it.copy(errorMessage = "No report available to share.") }
            return
        }

        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Financial Report - ${_uiState.value.selectedPeriod.label}")
                putExtra(Intent.EXTRA_TEXT, reportText)
            }
            val chooser = Intent.createChooser(sendIntent, "Share Report via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(errorMessage = "Unable to share report: ${e.message ?: "No sharing app found"}")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun calculateEpochRange(period: ReportPeriod): Pair<Long, Long> {
        val today = LocalDate.now()
        val toDay = today.toEpochDay()
        val fromDay = when (period) {
            ReportPeriod.LAST_7_DAYS -> today.minusDays(6).toEpochDay()
            ReportPeriod.THIS_MONTH -> today.withDayOfMonth(1).toEpochDay()
            ReportPeriod.LAST_30_DAYS -> today.minusDays(29).toEpochDay()
            ReportPeriod.ALL_TIME -> 0L
        }
        return fromDay to toDay
    }

    class Factory(
        private val repository: TransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReportViewModel(repository) as T
        }
    }
}