package com.tuapp.eventos.ui.eventdetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.eventos.databinding.ItemParticipantBinding
import com.tuapp.eventos.domain.model.Participant

class ParticipantAdapter : ListAdapter<Participant, ParticipantAdapter.ParticipantViewHolder>(ParticipantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val binding = ItemParticipantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParticipantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ParticipantViewHolder(private val binding: ItemParticipantBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(participant: Participant) {
            binding.tvParticipantName.text = participant.userName
            binding.tvParticipantRole.text = participant.roleId ?: "No Role"
        }
    }

    class ParticipantDiffCallback : DiffUtil.ItemCallback<Participant>() {
        override fun areItemsTheSame(oldItem: Participant, newItem: Participant): Boolean = oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: Participant, newItem: Participant): Boolean = oldItem == newItem
    }
}
