package com.tuapp.eventos.ui.events

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.eventos.data.model.GroupMember
import com.tuapp.eventos.data.model.Profile
import com.tuapp.eventos.databinding.ItemGroupMemberBinding

class MemberAdapter(
    private val isAdmin: Boolean = false,
    private val onRemoveClick: ((Pair<GroupMember, Profile>) -> Unit)? = null
) : ListAdapter<Pair<GroupMember, Profile>, MemberAdapter.MemberViewHolder>(MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemGroupMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position), isAdmin, onRemoveClick)
    }

    class MemberViewHolder(private val binding: ItemGroupMemberBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            data: Pair<GroupMember, Profile>,
            isAdmin: Boolean,
            onRemoveClick: ((Pair<GroupMember, Profile>) -> Unit)?
        ) {
            val member = data.first
            val profile = data.second

            binding.tvMemberName.text = profile.full_name ?: profile.username
            binding.tvMemberEmail.text = "@${profile.username}"
            binding.tvMemberRole.text = if (member.is_admin) "ADMIN" else "MEMBER"
            
            // Mostrar botón de borrar solo si el usuario es admin y no se está borrando a sí mismo o a otro admin
            binding.btnDeleteMember.visibility = if (isAdmin && !member.is_admin) View.VISIBLE else View.GONE

            binding.btnDeleteMember.setOnClickListener {
                onRemoveClick?.invoke(data)
            }
        }
    }

    class MemberDiffCallback : DiffUtil.ItemCallback<Pair<GroupMember, Profile>>() {
        override fun areItemsTheSame(
            oldItem: Pair<GroupMember, Profile>,
            newItem: Pair<GroupMember, Profile>
        ): Boolean = oldItem.first.user_id == newItem.first.user_id

        override fun areContentsTheSame(
            oldItem: Pair<GroupMember, Profile>,
            newItem: Pair<GroupMember, Profile>
        ): Boolean = oldItem == newItem
    }
}
