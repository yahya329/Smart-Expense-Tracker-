package com.example.myapplication.ui.theme // Adjust package

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartexpensetracker.ui.components.CategorySpendingItemView
import com.example.smartexpensetracker.viewmodel.AnalysisViewModel
import kotlin.collections.get

// import com.github.mikephil.charting.charts.PieChart (Requires adding the library to build.gradle)

class AnalysisScreen : Fragment() {

    private lateinit var viewModel: AnalysisViewModel
    private lateinit var tvTotalSpending: TextView
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Build the UI programmatically (instead of inflating an XML file)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        tvTotalSpending = TextView(context).apply {
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }
        root.addView(tvTotalSpending)

        // NOTE: To add the chart, uncomment this and add MPAndroidChart to build.gradle.kts
        /*
        val pieChart = PieChart(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600)
        }
        root.addView(pieChart)
        */

        recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        root.addView(recyclerView)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // You will need a ViewModelFactory here if your ViewModel requires the Repository in its constructor
        // viewModel = ViewModelProvider(this, YourFactory)[AnalysisViewModel::class.java]

        // For now, assuming a basic setup:
        // viewModel = ViewModelProvider(this)[AnalysisViewModel::class.java]

        observeData()
        viewModel.loadData()
    }

    private fun observeData() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state.isLoading) {
                tvTotalSpending.text = "Loading..."
                return@observe
            }

            if (state.errorMessage != null) {
                tvTotalSpending.text = state.errorMessage
                return@observe
            }

            // Display total spending
            tvTotalSpending.text = String.format("Total Spending: $%.2f", state.totalSpending)

            // Setup the RecyclerView with your reusable components
            val categories = state.categorySummary.toList()
            recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    // Using your reusable component!
                    val itemView = CategorySpendingItemView(parent.context)
                    return object : RecyclerView.ViewHolder(itemView) {}
                }

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val (category, amount) = categories[position]
                    (holder.itemView as CategorySpendingItemView).bind(category, amount)
                }

                override fun getItemCount() = categories.size
            }
        }
    }
}