package com.tuapp.eventos.ui.events

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.eventos.data.model.Profile
import com.tuapp.eventos.databinding.ItemUserSearchBinding

class UserSearchAdapter(
    private val onUserClick: (Profile) -> Unit
) : ListAdapter<Profile, UserSearchAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position), onUserClick)
    }

    class UserViewHolder(private val binding: ItemUserSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: Profile, onUserClick: (Profile) -> Unit) {
            binding.tvUserName.text = user.full_name ?: user.username
            binding.tvUserEmail.text = "@${user.username}"
            
            binding.btnInvite.setOnClickListener { 
                onUserClick(user) 
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<Profile>() {
        override fun areItemsTheSame(oldItem: Profile, newItem: Profile): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Profile, newItem: Profile): Boolean = oldItem == newItem
    }
}
