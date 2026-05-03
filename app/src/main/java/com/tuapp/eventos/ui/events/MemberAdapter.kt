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
    private var isUserAdmin: Boolean = false,
    private var isUserOwner: Boolean = false,
    private var groupOwnerId: String? = null,
    private val onPromoteAdmin: ((String, Boolean) -> Unit)? = null,
    private val onRemoveMember: ((String) -> Unit)? = null
) : ListAdapter<Pair<GroupMember, Profile>, MemberAdapter.MemberViewHolder>(MemberDiffCallback()) {

    fun setAdminStatus(isAdmin: Boolean, isOwner: Boolean, ownerId: String?) {
        this.isUserAdmin = isAdmin
        this.isUserOwner = isOwner
        this.groupOwnerId = ownerId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemGroupMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position), isUserAdmin, isUserOwner, groupOwnerId, onPromoteAdmin, onRemoveMember)
    }

    class MemberViewHolder(private val binding: ItemGroupMemberBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            data: Pair<GroupMember, Profile>,
            isCurrentUserAdmin: Boolean,
            isCurrentUserOwner: Boolean,
            groupOwnerId: String?,
            onPromoteAdmin: ((String, Boolean) -> Unit)?,
            onRemoveMember: ((String) -> Unit)?
        ) {
            val member = data.first
            val profile = data.second
            val userId = member.user_id

            binding.tvMemberName.text = profile.full_name ?: profile.username
            binding.tvMemberEmail.text = "@${profile.username}"
            
            // Badges logic (Creator/Admin)
            val isTargetOwner = userId.equals(groupOwnerId, ignoreCase = true)
            binding.tvCreatorBadge.visibility = if (isTargetOwner) View.VISIBLE else View.GONE
            binding.tvAdminBadge.visibility = if (member.is_admin && !isTargetOwner) View.VISIBLE else View.GONE
            
            android.util.Log.d("MemberAdapter", "User: ${profile.username}, isAdmin: ${member.is_admin}, isOwner: $isTargetOwner, ownerId: $groupOwnerId")
            
            // Click simple para gestionar miembro (más intuitivo que Long Click)
            binding.root.setOnClickListener {
                if (isCurrentUserAdmin && !isTargetOwner) {
                    val context = binding.root.context
                    val items = mutableListOf<String>()
                    
                    if (member.is_admin) {
                        // Solo el creador puede quitar el rango de admin
                        if (isCurrentUserOwner) {
                            items.add("Quitar Admin")
                        }
                    } else {
                        items.add("Hacer Admin")
                    }
                    
                    items.add("Eliminar del grupo")
                    
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                        .setTitle("Gestionar Miembro")
                        .setItems(items.toTypedArray()) { _, which ->
                            val selectedAction = items[which]
                            when (selectedAction) {
                                "Quitar Admin" -> onPromoteAdmin?.invoke(userId, false)
                                "Hacer Admin" -> onPromoteAdmin?.invoke(userId, true)
                                "Eliminar del grupo" -> onRemoveMember?.invoke(userId)
                            }
                        }
                        .show()
                }
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
