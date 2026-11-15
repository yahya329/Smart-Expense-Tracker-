
package com.example.myapplication.viewmodel

data class BudgetUiState(
    val monthlyBudget: Double = 0.0,
    val spentAmount: Double = 0.0,
    val budgetInput: String = "",
    val errorMessage: String? = null,
    val isSaved: Boolean = false
) {

    val remainingBudget: Double
        get() = (monthlyBudget - spentAmount).coerceAtLeast(0.0)

    val amountOverBudget: Double
        get() = (spentAmount - monthlyBudget).coerceAtLeast(0.0)

    val spendingProgress: Float
        get() = if (monthlyBudget <= 0.0) {
            0f
        } else {
            (spentAmount / monthlyBudget).toFloat()
                .coerceIn(0f, 1f)
        }

    val spendingPercentage: Int
        get() = if (monthlyBudget <= 0.0) {
            0
        } else {
            ((spentAmount / monthlyBudget) * 100).toInt()
        }

    val isNearLimit: Boolean
        get() = spendingPercentage >= 80 && spendingPercentage < 100

    val isOverBudget: Boolean
        get() = monthlyBudget > 0.0 && spentAmount >= monthlyBudget
}
