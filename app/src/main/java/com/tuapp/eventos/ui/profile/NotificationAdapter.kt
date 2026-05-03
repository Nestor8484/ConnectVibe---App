package com.tuapp.eventos.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.eventos.data.model.Notification
import com.tuapp.eventos.databinding.ItemNotificationBinding

class NotificationAdapter(
    private val onAccept: (Notification) -> Unit,
    private val onDecline: (Notification) -> Unit,
    private val onDelete: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position), onAccept, onDecline, onDelete)
    }

    class NotificationViewHolder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            notification: Notification,
            onAccept: (Notification) -> Unit,
            onDecline: (Notification) -> Unit,
            onDelete: (Notification) -> Unit
        ) {
            when (notification.type) {
                "group_invitation" -> {
                    binding.tvNotificationTitle.text = "Invitación a Grupo"
                    binding.tvNotificationTitle.setTextColor(android.graphics.Color.parseColor("#0D47A1"))
                    binding.tvNotificationMessage.text = "Te han invitado al grupo '${notification.group_name ?: "Desconocido"}'"
                    binding.llActions.visibility = View.VISIBLE
                    binding.btnDelete.visibility = View.GONE
                }
                "task_reminder" -> {
                    binding.tvNotificationTitle.text = "Aviso de Tarea"
                    binding.tvNotificationTitle.setTextColor(android.graphics.Color.parseColor("#E65100"))
                    val eventStr = if (!notification.event_name.isNullOrBlank()) " del evento '${notification.event_name}'" else ""
                    binding.tvNotificationMessage.text = "Tienes una nueva tarea pendiente: ${notification.task_title ?: "Nueva tarea"}$eventStr"
                    binding.llActions.visibility = View.VISIBLE // Asegurar que sea visible si hay acciones, pero en el original era GONE
                    // Re-checking original: binding.llActions.visibility = View.GONE
                    binding.llActions.visibility = View.GONE
                    binding.btnDelete.visibility = View.VISIBLE
                }
            }
            
            binding.btnAccept.setOnClickListener { onAccept(notification) }
            binding.btnDecline.setOnClickListener { onDecline(notification) }
            binding.btnDelete.setOnClickListener { onDelete(notification) }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean = 
            oldItem.id == newItem.id
        
        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean =
            oldItem == newItem
    }
}
