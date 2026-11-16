package com.example.myapplication.viewmodel

import com.example.myapplication.data.Category
import com.example.myapplication.data.TransactionType
import java.time.LocalDate

data class AddEditUiState(
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: Category = Category.FOOD,
    val date: String = LocalDate.now().toString(),
    val note: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val amountError: String? = null,
    val dateError: String? = null
)