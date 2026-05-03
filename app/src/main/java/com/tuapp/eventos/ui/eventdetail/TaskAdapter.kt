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
import com.tuapp.eventos.R
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
            
            // Effect: Strikethrough if completed
            binding.tvTaskTitle.paintFlags = if (isCompleted) {
                binding.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // Status Badge
            binding.tvTaskStatusBadge.text = when (task.status) {
                "in_progress" -> "EN CURSO"
                "completed" -> "HECHO"
                else -> "PENDIENTE"
            }

            // Colors based on status
            val statusColor = when (task.status) {
                "in_progress" -> Color.parseColor("#EF6C00") // Orange
                "completed" -> Color.parseColor("#2E7D32") // Green
                else -> Color.parseColor("#757575") // Grey
            }
            
            binding.tvTaskStatusBadge.setTextColor(statusColor)
            (binding.tvTaskStatusBadge.background as? GradientDrawable)?.setStroke(
                (1 * binding.root.resources.displayMetrics.density).toInt(),
                statusColor
            )

            // Role Tag and Colors
            binding.tvRoleTag.text = role?.name ?: "Sin rol"
            val roleColorStr = role?.color ?: "#1565C0"
            
            try {
                val roleColor = Color.parseColor(roleColorStr)
                binding.tvRoleTag.setTextColor(roleColor)
                
                val roleBg = binding.tvRoleTag.background.mutate() as? GradientDrawable
                roleBg?.let { bg ->
                    val alphaColor = (0.10 * 255).toInt() shl 24 or (roleColor and 0x00FFFFFF)
                    bg.setColor(alphaColor)
                    bg.setStroke((1.5 * binding.root.resources.displayMetrics.density).toInt(), roleColor)
                }

                binding.ivTaskIcon.imageTintList = ColorStateList.valueOf(roleColor)
                (binding.ivTaskIcon.background as? GradientDrawable)?.setStroke(
                    (1 * binding.root.resources.displayMetrics.density).toInt(),
                    roleColor
                )

                val cardView = binding.root as? com.google.android.material.card.MaterialCardView
                cardView?.strokeColor = roleColor
                cardView?.setCardBackgroundColor((0.02 * 255).toInt() shl 24 or (roleColor and 0x00FFFFFF))

            } catch (e: Exception) {}

            binding.root.setOnClickListener { onTaskClick(task) }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<EventTask>() {
        override fun areItemsTheSame(oldItem: EventTask, newItem: EventTask): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: EventTask, newItem: EventTask): Boolean = oldItem == newItem
    }
}
