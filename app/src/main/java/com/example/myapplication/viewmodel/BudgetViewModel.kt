
package com.example.myapplication.viewmodel

import com.example.myapplication.data.BudgetSettingsStore

class BudgetViewModel(
    private val settingsRepository: BudgetSettingsStore
) {

    var uiState: BudgetUiState = loadSavedBudget()
        private set

    private fun loadSavedBudget(): BudgetUiState {
        val savedBudget = settingsRepository.getMonthlyBudget()

        return BudgetUiState(
            monthlyBudget = savedBudget,
            budgetInput = if (savedBudget > 0.0) {
                savedBudget.toString()
            } else {
                ""
            }
        )
    }

    fun onBudgetInputChanged(newValue: String) {
        val validInput = newValue.all {
            it.isDigit() || it == '.' || it == ','
        }

        val hasOneDecimalSeparator =
            newValue.count { it == '.' || it == ',' } <= 1

        if (validInput && hasOneDecimalSeparator) {
            uiState = uiState.copy(
                budgetInput = newValue,
                errorMessage = null,
                isSaved = false
            )
        }
    }

    fun saveBudget(): BudgetUiState {
        return saveMonthlyBudget(uiState.budgetInput)
    }

    fun saveMonthlyBudget(input: String): BudgetUiState {
        val amount = input.trim()
            .replace(',', '.')
            .toDoubleOrNull()

        if (amount == null || !amount.isFinite() || amount <= 0.0) {
            uiState = uiState.copy(
                budgetInput = input,
                errorMessage = "Enter a valid amount greater than zero.",
                isSaved = false
            )
            return uiState
        }

        return try {
            settingsRepository.saveMonthlyBudget(amount)

            uiState = uiState.copy(
                monthlyBudget = amount,
                budgetInput = amount.toString(),
                errorMessage = null,
                isSaved = true
            )

            uiState
        } catch (exception: Exception) {
            uiState = uiState.copy(
                errorMessage = "Could not save the budget. Please try again.",
                isSaved = false
            )

            uiState
        }
    }

    fun updateSpentAmount(amount: Double): BudgetUiState {
        if (!amount.isFinite() || amount < 0.0) {
            return uiState
        }

        uiState = uiState.copy(
            spentAmount = amount
        )

        return uiState
    }
}
