package com.tuapp.eventos.ui.eventdetail

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.eventos.databinding.ItemTaskBinding
import com.tuapp.eventos.domain.model.EventTask
import com.tuapp.eventos.domain.model.Role

class TaskAdapter(
    private val onTaskClick: (EventTask) -> Unit,
    private val onTaskStatusChange: (EventTask, Boolean) -> Unit
) : ListAdapter<EventTask, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    private var roles: List<Role> = emptyList()

    fun setRoles(roles: List<Role>) {
        this.roles = roles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        val role = roles.find { it.id == task.roleId }
        holder.bind(task, role, onTaskClick, onTaskStatusChange)
    }

    class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            task: EventTask,
            role: Role?,
            onTaskClick: (EventTask) -> Unit,
            onTaskStatusChange: (EventTask, Boolean) -> Unit
        ) {
            binding.tvTaskTitle.text = task.title
            
            val isCompleted = task.status == "completed" || task.isCompleted
            binding.tvTaskTitle.paintFlags = if (isCompleted) {
                binding.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            binding.cbTaskStatus.setOnCheckedChangeListener(null)
            binding.cbTaskStatus.isChecked = isCompleted
            binding.cbTaskStatus.setOnCheckedChangeListener { _, isChecked ->
                onTaskStatusChange(task, isChecked)
            }

            binding.tvTaskStatusLabel.text = when (task.status) {
                "in_progress" -> "En curso"
                "completed" -> "Completada"
                else -> "Pendiente"
            }
            
            val statusColor = when (task.status) {
                "in_progress" -> Color.parseColor("#EF6C00") // Naranja oscuro
                "completed" -> Color.parseColor("#2E7D32") // Verde oscuro
                else -> Color.parseColor("#757575") // Gris
            }
            binding.tvTaskStatusLabel.setTextColor(statusColor)

            binding.tvRoleTag.text = role?.name ?: "Sin rol"
            
            val colorStr = role?.color ?: "#1565C0"
            try {
                val colorInt = Color.parseColor(colorStr)
                val alphaColor = (0.15 * 255).toInt() shl 24 or (colorInt and 0x00FFFFFF)
                
                binding.cbTaskStatus.buttonTintList = ColorStateList.valueOf(colorInt)
                binding.tvRoleTag.setTextColor(colorInt)
                
                val roleBg = binding.tvRoleTag.background.mutate() as? GradientDrawable
                roleBg?.let { bg ->
                    bg.setColor(alphaColor)
                    bg.setStroke(2, colorInt)
                }

                val cardView = binding.root as? com.google.android.material.card.MaterialCardView
                cardView?.strokeColor = colorInt
                cardView?.strokeWidth = (1.5 * binding.root.resources.displayMetrics.density).toInt()
                cardView?.setCardBackgroundColor((0.05 * 255).toInt() shl 24 or (colorInt and 0x00FFFFFF))

            } catch (e: Exception) {}

            binding.root.setOnClickListener { onTaskClick(task) }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<EventTask>() {
        override fun areItemsTheSame(oldItem: EventTask, newItem: EventTask): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: EventTask, newItem: EventTask): Boolean = oldItem == newItem
    }
}
