package com.tuapp.eventos.ui.events

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.eventos.data.model.Group
import com.tuapp.eventos.databinding.ItemEventBinding

class GroupAdapter(
    private val onGroupClick: (Group) -> Unit
) : ListAdapter<Group, GroupAdapter.GroupViewHolder>(GroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position), onGroupClick)
    }

    class GroupViewHolder(private val binding: ItemEventBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(group: Group, onGroupClick: (Group) -> Unit) {
            binding.tvEventTitle.text = group.name
            binding.tvEventLocation.text = "Grupo de Amigos" // Or any other description
            binding.tvEventDate.text = ""
            
            // Reusing item_event layout fields
            binding.ivEventIcon.setImageResource(android.R.drawable.ic_menu_myplaces)

            binding.root.setOnClickListener { 
                onGroupClick(group) 
            }
        }
    }

    class GroupDiffCallback : DiffUtil.ItemCallback<Group>() {
        override fun areItemsTheSame(oldItem: Group, newItem: Group): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Group, newItem: Group): Boolean = oldItem == newItem
    }
}
