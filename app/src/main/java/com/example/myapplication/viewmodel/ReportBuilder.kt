package com.example.myapplication.viewmodel

import com.example.myapplication.data.Category
import com.example.myapplication.data.TransactionEntity
import com.example.myapplication.data.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ReportSummary(
    val periodLabel: String,
    val fromDate: String,
    val toDate: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double,
    val transactionCount: Int,
    val categoryTotals: Map<Category, Double>
)

class ReportBuilder {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    fun buildSummary(
        transactions: List<TransactionEntity>,
        periodLabel: String,
        fromEpochDay: Long,
        toEpochDay: Long
    ): ReportSummary {
        val totalIncomeCents = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amountCents }

        val totalExpenseCents = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amountCents }

        val categoryBreakdown = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.amountCents } / 100.0 }
            .toList()
            .sortedByDescending { it.second }
            .toMap()

        return ReportSummary(
            periodLabel = periodLabel,
            fromDate = LocalDate.ofEpochDay(fromEpochDay).format(dateFormatter),
            toDate = LocalDate.ofEpochDay(toEpochDay).format(dateFormatter),
            totalIncome = totalIncomeCents / 100.0,
            totalExpense = totalExpenseCents / 100.0,
            netBalance = (totalIncomeCents - totalExpenseCents) / 100.0,
            transactionCount = transactions.size,
            categoryTotals = categoryBreakdown
        )
    }

    fun buildReadableReport(summary: ReportSummary): String {
        val sb = StringBuilder()
        sb.appendLine("📊 Smart Expense Tracker — Financial Report")
        sb.appendLine("Period: ${summary.periodLabel} (${summary.fromDate} to ${summary.toDate})")
        sb.appendLine("----------------------------------------")
        sb.appendLine(String.format(Locale.US, "Total Income:   $%.2f", summary.totalIncome))
        sb.appendLine(String.format(Locale.US, "Total Expenses: $%.2f", summary.totalExpense))
        sb.appendLine(String.format(Locale.US, "Net Balance:    $%.2f", summary.netBalance))
        sb.appendLine("Transactions:   ${summary.transactionCount}")

        if (summary.categoryTotals.isNotEmpty()) {
            sb.appendLine("\nSpending by Category:")
            summary.categoryTotals.forEach { (category, amount) ->
                sb.appendLine(String.format(Locale.US, "• %s: $%.2f", category.name, amount))
            }
        } else {
            sb.appendLine("\nNo expenses recorded in this period.")
        }

        return sb.toString()
    }
}