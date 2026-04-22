package com.tuapp.eventos.ui.eventdetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.eventos.databinding.ItemExpenseBinding
import com.tuapp.eventos.domain.model.Expense

class ExpenseAdapter(
    private val onExpenseClick: (Expense) -> Unit
) : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position), onExpenseClick)
    }

    class ExpenseViewHolder(private val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(expense: Expense, onExpenseClick: (Expense) -> Unit) {
            binding.tvExpenseName.text = expense.description
            binding.tvExpenseAmount.text = String.format("%.2f€", expense.amount)
            binding.tvExpenseType.text = "Gasto" // Podrías añadir un campo 'category' al modelo si quieres
            
            binding.root.setOnClickListener { onExpenseClick(expense) }
        }
    }

    class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean = oldItem == newItem
    }
}
