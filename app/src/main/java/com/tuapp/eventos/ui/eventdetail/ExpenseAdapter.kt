package com.tuapp.eventos.ui.eventdetail

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.eventos.databinding.ItemExpenseBinding
import com.tuapp.eventos.domain.model.Expense

class ExpenseAdapter(
    private var eventColor: String? = null,
    private val onExpenseClick: (Expense) -> Unit
) : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    fun updateEventColor(color: String?) {
        this.eventColor = color
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position), eventColor, onExpenseClick)
    }

    class ExpenseViewHolder(private val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(expense: Expense, eventColor: String?, onExpenseClick: (Expense) -> Unit) {
            binding.tvExpenseName.text = expense.title
            binding.tvExpenseAmount.text = String.format("%.2f€", expense.amount)
            binding.tvExpenseType.text = expense.description ?: "Otros"
            
            eventColor?.let { colorStr ->
                try {
                    val colorInt = colorStr.toColorInt()
                    val alphaColor = (0.15 * 255).toInt() shl 24 or (colorInt and 0x00FFFFFF)
                    
                    binding.ivExpenseIcon.imageTintList = ColorStateList.valueOf(colorInt)
                    binding.tvExpenseAmount.setTextColor(colorInt)
                    
                    // Estilo de la etiqueta de categoría (fondo suave + texto color evento)
                    val categoryBg = binding.tvExpenseType.background.mutate() as? android.graphics.drawable.GradientDrawable
                    categoryBg?.let { bg ->
                        bg.setColor(alphaColor)
                        bg.setStroke(2, colorInt)
                    }
                    binding.tvExpenseType.setTextColor(colorInt)
                } catch (e: Exception) {
                    // Fallback
                }
            }
            
            binding.root.setOnClickListener { onExpenseClick(expense) }
        }
    }

    class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean = oldItem == newItem
    }
}
