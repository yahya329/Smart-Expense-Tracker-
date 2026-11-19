package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.Category
import com.example.myapplication.data.TransactionEntity
import com.example.myapplication.data.TransactionRepository
import com.example.myapplication.data.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddEditViewModel(
    private val repository: TransactionRepository,
    private val transactionId: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddEditUiState(isEditMode = transactionId != null)
    )
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        if (transactionId != null) {
            loadTransaction(transactionId)
        }
    }

    fun onAmountChange(value: String) {
        _uiState.update {
            it.copy(amount = value, amountError = null, errorMessage = null)
        }
    }

    fun onTypeChange(value: TransactionType) {
        _uiState.update { it.copy(type = value) }
    }

    fun onCategoryChange(value: Category) {
        _uiState.update { it.copy(category = value) }
    }

    fun onDateChange(value: String) {
        _uiState.update {
            it.copy(date = value, dateError = null, errorMessage = null)
        }
    }

    fun onNoteChange(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    private fun loadTransaction(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val transaction = repository.getTransaction(id)

                if (transaction == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Transaction not found."
                        )
                    }
                    return@launch
                }

                val amountText = BigDecimal.valueOf(transaction.amountCents, 2)
                    .stripTrailingZeros()
                    .toPlainString()

                _uiState.update {
                    it.copy(
                        amount = amountText,
                        type = transaction.type,
                        category = transaction.category,
                        date = LocalDate.ofEpochDay(transaction.dateEpochDay).toString(),
                        note = transaction.note,
                        isLoading = false
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message
                            ?: "Could not load transaction."
                    )
                }
            }
        }
    }

    fun save() {
        val current = _uiState.value

        if (current.isSaving || current.isLoading || current.isSaved) return

        var amountError: String? = null
        var dateError: String? = null

        val amountCents = try {
            val amount = BigDecimal(current.amount.trim())
            val cents = amount.setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact()

            if (cents <= 0) {
                amountError = "Amount must be greater than zero."
                null
            } else {
                cents
            }
        } catch (exception: Exception) {
            amountError = "Enter a valid amount (maximum 2 decimal places)."
            null
        }

        val dateEpochDay = try {
            LocalDate.parse(current.date.trim()).toEpochDay()
        } catch (exception: Exception) {
            dateError = "Enter a valid date in YYYY-MM-DD format."
            null
        }

        _uiState.update {
            it.copy(
                amountError = amountError,
                dateError = dateError,
                errorMessage = null
            )
        }

        if (amountCents == null || dateEpochDay == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            try {
                val transaction = TransactionEntity(
                    id = transactionId ?: 0L,
                    amountCents = amountCents,
                    type = current.type,
                    category = current.category,
                    note = current.note.trim(),
                    dateEpochDay = dateEpochDay
                )

                if (transactionId == null) {
                    repository.addTransaction(transaction)
                } else {
                    repository.updateTransaction(transaction)
                }

                _uiState.update {
                    it.copy(isSaving = false, isSaved = true)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = exception.message
                            ?: "Could not save transaction. Please try again."
                    )
                }
            }
        }
    }
}