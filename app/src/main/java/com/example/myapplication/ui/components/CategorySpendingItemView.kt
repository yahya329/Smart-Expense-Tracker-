package com.example.myapplication.ui.components // Adjust package

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class CategorySpendingItemView(context: Context) : LinearLayout(context) {

    private val tvCategory: TextView
    private val tvAmount: TextView

    init {
        // Setup the layout programmatically (simulating an XML layout)
        orientation = HORIZONTAL
        setPadding(32, 24, 32, 24)
        gravity = Gravity.CENTER_VERTICAL

        tvCategory = TextView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            textSize = 16f
        }

        tvAmount = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            textSize = 16f
        }

        addView(tvCategory)
        addView(tvAmount)
    }

    // This makes it configurable and independent
    fun bind(categoryName: String, amount: Double) {
        tvCategory.text = categoryName
        tvAmount.text = String.format("$%.2f", amount)
    }
}