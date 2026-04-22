package com.tuapp.eventos.ui.events

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.eventos.databinding.ItemGroupMemberBinding
import com.tuapp.eventos.domain.model.GroupMember
import com.tuapp.eventos.domain.model.MemberRole

class MemberAdapter(
    private val isAdmin: Boolean,
    private val onDeleteClick: (GroupMember) -> Unit
) : ListAdapter<GroupMember, MemberAdapter.MemberViewHolder>(MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemGroupMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MemberViewHolder(private val binding: ItemGroupMemberBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(member: GroupMember) {
            binding.tvMemberName.text = member.userName
            binding.tvMemberEmail.text = member.email
            binding.tvMemberRole.text = member.role.name
            
            // Mostrar botón de eliminar solo si el usuario actual es ADMIN 
            // y no se está intentando eliminar a sí mismo (simplificado por ahora)
            binding.btnDeleteMember.visibility = if (isAdmin && member.role != MemberRole.ADMIN) View.VISIBLE else View.GONE
            
            binding.btnDeleteMember.setOnClickListener {
                onDeleteClick(member)
            }
        }
    }

    class MemberDiffCallback : DiffUtil.ItemCallback<GroupMember>() {
        override fun areItemsTheSame(oldItem: GroupMember, newItem: GroupMember): Boolean = oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: GroupMember, newItem: GroupMember): Boolean = oldItem == newItem
    }
}
