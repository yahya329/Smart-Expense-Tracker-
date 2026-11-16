package com.example.myapplication.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.myapplication.viewmodel.TransactionDateFilter
import com.example.myapplication.viewmodel.TransactionListItem
import com.example.myapplication.viewmodel.TransactionTypeFilter
import com.example.myapplication.viewmodel.TransactionsUiState
import com.example.myapplication.viewmodel.TransactionsViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Integration entry point. The owner of navigation creates the ViewModel once
 * the leader's shared TransactionRepository contract has been completed.
 */
@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    TransactionsScreen(
        state = state,
        onSearchChange = viewModel::onSearchQueryChanged,
        onTypeChange = viewModel::onTypeFilterChanged,
        onCategoryChange = viewModel::onCategoryFilterChanged,
        onDateChange = viewModel::onDateFilterChanged,
        onDeleteConfirmed = viewModel::deleteConfirmed,
        onRefresh = viewModel::refresh,
        onDismissError = viewModel::clearActionError,
        modifier = modifier
    )
}

/** Stateless, previewable UI that never owns or creates a transaction database. */
@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onSearchChange: (String) -> Unit,
    onTypeChange: (TransactionTypeFilter) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onDateChange: (TransactionDateFilter) -> Unit,
    onDeleteConfirmed: (String) -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var deleteCandidate by remember { mutableStateOf<TransactionListItem?>(null) }
    deleteCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { if (!state.isDeleting) deleteCandidate = null },
            title = { Text("Delete transaction?") },
            text = {
                Text("Delete ${candidate.description.ifBlank { candidate.category }}? This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isDeleting,
                    onClick = {
                        onDeleteConfirmed(candidate.id)
                        deleteCandidate = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isDeleting,
                    onClick = { deleteCandidate = null }
                ) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Transaction history", style = MaterialTheme.typography.headlineSmall)
                    Text("Review, search and manage your records", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onRefresh) { Text("Refresh") }
            }
        }
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                label = { Text("Search description or note") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                TransactionTypeFilter.entries.forEach { type ->
                    FilterChip(
                        selected = state.selectedType == type,
                        onClick = { onTypeChange(type) },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    var categoriesOpen by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { categoriesOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            state.selectedCategory ?: "All categories",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    DropdownMenu(
                        expanded = categoriesOpen,
                        onDismissRequest = { categoriesOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All categories") },
                            onClick = {
                                onCategoryChange(null)
                                categoriesOpen = false
                            }
                        )
                        state.availableCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    onCategoryChange(category)
                                    categoriesOpen = false
                                }
                            )
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    var datesOpen by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { datesOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(dateLabel(state.selectedDateFilter), maxLines = 1) }
                    DropdownMenu(
                        expanded = datesOpen,
                        onDismissRequest = { datesOpen = false }
                    ) {
                        TransactionDateFilter.entries.forEach { dateFilter ->
                            DropdownMenuItem(
                                text = { Text(dateLabel(dateFilter)) },
                                onClick = {
                                    onDateChange(dateFilter)
                                    datesOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TotalsCard(
                    label = "Income",
                    value = state.totalIncome,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
                TotalsCard(
                    label = "Expenses",
                    value = state.totalExpenses,
                    color = Color(0xFFC62828),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (state.isDeleting) {
            item { Text("Deleting transaction...", style = MaterialTheme.typography.bodySmall) }
        }
        if (state.actionErrorMessage != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(state.actionErrorMessage, modifier = Modifier.weight(1f))
                        TextButton(onClick = onDismissError) { Text("Dismiss") }
                    }
                }
            }
        }
        when {
            state.isLoading -> item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.errorMessage != null -> item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Could not load transactions", style = MaterialTheme.typography.titleMedium)
                    Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRefresh) { Text("Try again") }
                }
            }
            state.visibleTransactions.isEmpty() -> item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("No transactions found", style = MaterialTheme.typography.titleMedium)
                    Text("Try changing your search or filters.")
                }
            }
            else -> {
                item {
                    Text(
                        "${state.visibleTransactions.size} transactions",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(state.visibleTransactions, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        canDelete = !state.isDeleting,
                        onDelete = { deleteCandidate = transaction }
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalsCard(label: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                "${formatAmount(value)} EGP",
                color = color,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionListItem,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        transaction.description.ifBlank { transaction.category },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${if (transaction.isIncome) "Income" else "Expense"} · ${transaction.category}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "${if (transaction.isIncome) "+" else "−"}${formatAmount(abs(transaction.amount))} EGP",
                    color = if (transaction.isIncome) Color(0xFF2E7D32) else Color(0xFFC62828),
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.dateMillis)),
                style = MaterialTheme.typography.bodySmall
            )
            if (transaction.note.isNotBlank()) {
                Text(transaction.note, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
            }
            TextButton(enabled = canDelete, onClick = onDelete, modifier = Modifier.align(Alignment.End)) {
                Text("Delete")
            }
        }
    }
}

private fun formatAmount(value: Double): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(value)

private fun dateLabel(filter: TransactionDateFilter): String = when (filter) {
    TransactionDateFilter.ALL -> "All dates"
    TransactionDateFilter.LAST_7_DAYS -> "Last 7 days"
    TransactionDateFilter.LAST_30_DAYS -> "Last 30 days"
    TransactionDateFilter.THIS_MONTH -> "This month"
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun TransactionsScreenPreview() {
    // Preview-only fake data; no database or shared Repository implementation is changed.
    val demoTransactions = remember {
        val now = System.currentTimeMillis()
        val day = 24L * 60L * 60L * 1000L
        MutableStateFlow(
            listOf(
                TransactionListItem("1", 15000.0, true, "Salary", now, "November salary"),
                TransactionListItem("2", 800.0, false, "Food", now - 2L * day, "Groceries", "Weekly shopping"),
                TransactionListItem("3", 150.0, false, "Transport", now - 50L * day, "Bus fare")
            )
        )
    }
    val demoViewModel = remember {
        TransactionsViewModel(
            repository = object : TransactionRepository {},
            observeHistory = { demoTransactions },
            deleteFromHistory = { _, id ->
                demoTransactions.value = demoTransactions.value.filterNot { it.id == id }
            },
            // Avoid an Android main-dispatcher dependency in the design-time preview.
            dispatcher = Dispatchers.Unconfined
        )
    }
    MaterialTheme {
        TransactionsScreen(viewModel = demoViewModel)
    }
}
