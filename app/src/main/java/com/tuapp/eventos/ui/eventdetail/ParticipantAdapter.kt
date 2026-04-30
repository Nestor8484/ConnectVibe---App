package com.tuapp.eventos.ui.eventdetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.eventos.databinding.ItemParticipantBinding
import com.tuapp.eventos.domain.model.GroupMember

class ParticipantAdapter : ListAdapter<GroupMember, ParticipantAdapter.ParticipantViewHolder>(ParticipantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val binding = ItemParticipantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParticipantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ParticipantViewHolder(private val binding: ItemParticipantBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(participant: GroupMember) {
            binding.tvParticipantName.text = participant.userName
            binding.tvParticipantRole.text = participant.role.name
        }
    }

    class ParticipantDiffCallback : DiffUtil.ItemCallback<GroupMember>() {
        override fun areItemsTheSame(oldItem: GroupMember, newItem: GroupMember): Boolean = oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: GroupMember, newItem: GroupMember): Boolean = oldItem == newItem
    }
}
