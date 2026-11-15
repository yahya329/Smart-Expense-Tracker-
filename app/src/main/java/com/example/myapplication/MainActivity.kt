
package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.myapplication.data.SettingsRepository
import com.example.myapplication.ui.BudgetScreen
import com.example.myapplication.viewmodel.BudgetViewModel

class MainActivity : ComponentActivity() {

    private lateinit var budgetScreen: BudgetScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_budget)

        val settingsRepository = SettingsRepository(this)

        val budgetViewModel = BudgetViewModel(
            settingsRepository
        )

        budgetScreen = BudgetScreen(
            activity = this,
            viewModel = budgetViewModel
        )

        budgetScreen.setup()

        // Expenses will be connected to the Transactions feature later.
        budgetScreen.updateSpentAmount(0.0)
    }
}
