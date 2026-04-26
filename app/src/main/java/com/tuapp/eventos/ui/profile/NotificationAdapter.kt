package com.tuapp.eventos.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.eventos.data.model.Group
import com.tuapp.eventos.data.model.Notification
import com.tuapp.eventos.databinding.ItemNotificationBinding

class NotificationAdapter(
    private val onAccept: (Notification) -> Unit,
    private val onDecline: (Notification) -> Unit
) : ListAdapter<Pair<Notification, Group>, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position), onAccept, onDecline)
    }

    class NotificationViewHolder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: Pair<Notification, Group>, onAccept: (Notification) -> Unit, onDecline: (Notification) -> Unit) {
            val notification = data.first
            val group = data.second

            binding.tvNotificationMessage.text = "Te han invitado al grupo '${group.name}'"
            
            binding.btnAccept.setOnClickListener { onAccept(notification) }
            binding.btnDecline.setOnClickListener { onDecline(notification) }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<Pair<Notification, Group>>() {
        override fun areItemsTheSame(oldItem: Pair<Notification, Group>, newItem: Pair<Notification, Group>): Boolean = 
            oldItem.first.id == newItem.first.id
        
        override fun areContentsTheSame(oldItem: Pair<Notification, Group>, newItem: Pair<Notification, Group>): Boolean = 
            oldItem == newItem
    }
}
