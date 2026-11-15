
package com.example.myapplication.data

import android.content.Context

interface BudgetSettingsStore {
    fun getMonthlyBudget(): Double
    fun saveMonthlyBudget(amount: Double)
}

class SettingsRepository(context: Context) : BudgetSettingsStore {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    override fun getMonthlyBudget(): Double {
        val savedValue = preferences.getLong(KEY_MONTHLY_BUDGET, 0L)

        return if (savedValue == 0L) {
            0.0
        } else {
            java.lang.Double.longBitsToDouble(savedValue)
        }
    }

    override fun saveMonthlyBudget(amount: Double) {
        preferences.edit()
            .putLong(
                KEY_MONTHLY_BUDGET,
                java.lang.Double.doubleToRawLongBits(amount)
            )
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "budget_settings"
        const val KEY_MONTHLY_BUDGET = "monthly_budget"
    }
}
