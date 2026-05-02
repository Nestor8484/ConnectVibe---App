package com.tuapp.eventos.ui.eventdetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.eventos.R
import com.tuapp.eventos.databinding.ItemParticipantBinding
import com.tuapp.eventos.domain.model.GroupMember
import com.tuapp.eventos.domain.model.MemberRole

class ParticipantAdapter(
    private val onAdminPromotion: (String, Boolean) -> Unit = { _, _ -> },
    private val onRemoveMember: (String) -> Unit = {}
) : ListAdapter<GroupMember, ParticipantAdapter.ParticipantViewHolder>(ParticipantDiffCallback()) {

    private var creatorId: String? = null
    private var isCurrentUserAdmin: Boolean = false

    fun setEventDetails(creatorId: String?, isCurrentUserAdmin: Boolean) {
        this.creatorId = creatorId
        this.isCurrentUserAdmin = isCurrentUserAdmin
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val binding = ItemParticipantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParticipantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(getItem(position), creatorId, isCurrentUserAdmin, onAdminPromotion, onRemoveMember)
    }

    class ParticipantViewHolder(private val binding: ItemParticipantBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            participant: GroupMember, 
            creatorId: String?, 
            isCurrentUserAdmin: Boolean,
            onAdminPromotion: (String, Boolean) -> Unit,
            onRemoveMember: (String) -> Unit
        ) {
            binding.tvParticipantName.text = participant.userName
            binding.tvParticipantRole.text = participant.email
            
            val isCreator = participant.userId == creatorId
            val isAdmin = participant.role == MemberRole.ADMIN
            
            binding.tvCreatorBadge.visibility = if (isCreator) View.VISIBLE else View.GONE
            binding.tvAdminBadge.visibility = if (isAdmin) View.VISIBLE else View.GONE
            
            if (isCurrentUserAdmin && !isCreator) {
                binding.root.setOnClickListener { view ->
                    val popup = PopupMenu(view.context, view)
                    val ctx = view.context
                    
                    if (isAdmin) {
                        popup.menu.add(ctx.getString(R.string.remove_admin))
                    } else {
                        popup.menu.add(ctx.getString(R.string.make_admin))
                    }
                    
                    popup.menu.add(ctx.getString(R.string.remove_from_event))
                    
                    popup.setOnMenuItemClickListener { item ->
                        when {
                            item.title == ctx.getString(R.string.make_admin) -> onAdminPromotion(participant.userId, true)
                            item.title == ctx.getString(R.string.remove_admin) -> onAdminPromotion(participant.userId, false)
                            item.title == ctx.getString(R.string.remove_from_event) -> {
                                com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
                                    .setTitle(R.string.remove_participant_title)
                                    .setMessage(R.string.confirm_remove_participant)
                                    .setPositiveButton(android.R.string.ok) { _, _ ->
                                        onRemoveMember(participant.userId)
                                    }
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .show()
                            }
                        }
                        true
                    }
                    popup.show()
                }
            } else {
                binding.root.setOnClickListener(null)
            }
        }
    }

    class ParticipantDiffCallback : DiffUtil.ItemCallback<GroupMember>() {
        override fun areItemsTheSame(oldItem: GroupMember, newItem: GroupMember): Boolean = oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: GroupMember, newItem: GroupMember): Boolean = oldItem == newItem
    }
}
