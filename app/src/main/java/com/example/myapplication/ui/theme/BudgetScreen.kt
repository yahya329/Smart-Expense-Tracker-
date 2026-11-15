package com.example.myapplication.ui

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.example.myapplication.R
import com.example.myapplication.viewmodel.BudgetViewModel
import java.util.Locale

class BudgetScreen(
    private val activity: Activity,
    private val viewModel: BudgetViewModel
) {

    private lateinit var budgetInput: EditText
    private lateinit var saveButton: Button

    private lateinit var saveMessage: TextView
    private lateinit var monthlyBudgetText: TextView
    private lateinit var totalSpentText: TextView
    private lateinit var remainingBudgetText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var warningText: TextView

    fun setup() {
        budgetInput = activity.findViewById(R.id.etMonthlyBudget)
        saveButton = activity.findViewById(R.id.btnSaveBudget)

        saveMessage = activity.findViewById(R.id.tvSaveMessage)
        monthlyBudgetText = activity.findViewById(R.id.tvMonthlyBudget)
        totalSpentText = activity.findViewById(R.id.tvTotalSpent)
        remainingBudgetText = activity.findViewById(R.id.tvRemainingBudget)
        progressBar = activity.findViewById(R.id.budgetProgress)
        progressText = activity.findViewById(R.id.tvProgressPercentage)
        warningText = activity.findViewById(R.id.tvBudgetWarning)

        budgetInput.setText(viewModel.uiState.budgetInput)

        budgetInput.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    val value = s?.toString().orEmpty()
                    viewModel.onBudgetInputChanged(value)
                    saveMessage.visibility = View.GONE
                    budgetInput.error = null
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) = Unit
            }
        )

        saveButton.setOnClickListener {
            val state = viewModel.saveMonthlyBudget(
                budgetInput.text.toString()
            )

            if (state.errorMessage != null) {
                budgetInput.error = state.errorMessage
                saveMessage.visibility = View.GONE
            } else {
                budgetInput.error = null
                saveMessage.text = "Budget saved successfully."
                saveMessage.setTextColor(Color.rgb(33, 150, 83))
                saveMessage.visibility = View.VISIBLE
            }

            renderScreen()
        }

        renderScreen()
    }

    fun updateSpentAmount(amount: Double) {
        viewModel.updateSpentAmount(amount)
        renderScreen()
    }

    private fun renderScreen() {
        val state = viewModel.uiState

        monthlyBudgetText.text = String.format(
            Locale.getDefault(),
            "Monthly limit: %.2f EGP",
            state.monthlyBudget
        )

        totalSpentText.text = String.format(
            Locale.getDefault(),
            "Amount spent: %.2f EGP",
            state.spentAmount
        )

        remainingBudgetText.text = String.format(
            Locale.getDefault(),
            "Remaining: %.2f EGP",
            state.remainingBudget
        )

        progressBar.progress = state.spendingPercentage.coerceIn(0, 100)
        progressText.text = "${state.spendingPercentage}% used"

        when {
            state.monthlyBudget <= 0.0 -> {
                warningText.visibility = View.GONE
                remainingBudgetText.setTextColor(Color.DKGRAY)
                setProgressColor(Color.GRAY)
            }

            state.spentAmount > state.monthlyBudget -> {
                warningText.text = String.format(
                    Locale.getDefault(),
                    "You are over budget by %.2f EGP.",
                    state.amountOverBudget
                )
                warningText.setTextColor(Color.RED)
                warningText.visibility = View.VISIBLE
                remainingBudgetText.setTextColor(Color.RED)
                setProgressColor(Color.RED)
            }

            state.isOverBudget -> {
                warningText.text =
                    "You have reached your monthly budget."
                warningText.setTextColor(Color.RED)
                warningText.visibility = View.VISIBLE
                remainingBudgetText.setTextColor(Color.RED)
                setProgressColor(Color.RED)
            }

            state.isNearLimit -> {
                warningText.text =
                    "You are getting close to your budget limit."
                warningText.setTextColor(Color.rgb(255, 152, 0))
                warningText.visibility = View.VISIBLE
                remainingBudgetText.setTextColor(Color.rgb(255, 152, 0))
                setProgressColor(Color.rgb(255, 152, 0))
            }

            else -> {
                warningText.visibility = View.GONE
                remainingBudgetText.setTextColor(
                    Color.rgb(33, 150, 83)
                )
                setProgressColor(Color.rgb(33, 150, 83))
            }
        }
    }

    private fun setProgressColor(color: Int) {
        progressBar.progressTintList = ColorStateList.valueOf(color)
    }
}
