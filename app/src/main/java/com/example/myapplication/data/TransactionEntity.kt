package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")

data class TransactionEntity (    @PrimaryKey(autoGenerate = true) val id: Long = 0,
                             val amountCents: Long,
                             val type: TransactionType,
                             val category: Category,
                             val note: String = "",
                             val dateEpochDay: Long)

