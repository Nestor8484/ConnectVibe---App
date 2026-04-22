package com.tuapp.eventos.ui.eventdetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.eventos.databinding.ItemRoleBinding
import com.tuapp.eventos.domain.model.Role

class RoleAdapter(
    private val onRoleClick: (Role) -> Unit
) : ListAdapter<Role, RoleAdapter.RoleViewHolder>(RoleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoleViewHolder {
        val binding = ItemRoleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoleViewHolder, position: Int) {
        holder.bind(getItem(position), onRoleClick)
    }

    class RoleViewHolder(private val binding: ItemRoleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(role: Role, onRoleClick: (Role) -> Unit) {
            binding.tvRoleName.text = role.name
            
            val iconRes = when (role.name) {
                "Logística" -> android.R.drawable.ic_menu_manage
                "Catering" -> android.R.drawable.ic_menu_view
                "Música" -> android.R.drawable.ic_lock_silent_mode_off
                "Invitados" -> android.R.drawable.ic_menu_agenda
                "Limpieza" -> android.R.drawable.ic_menu_delete
                "Fotografía" -> android.R.drawable.ic_menu_camera
                "Decoración" -> android.R.drawable.ic_menu_gallery
                "Seguridad" -> android.R.drawable.ic_lock_lock
                else -> android.R.drawable.ic_menu_help
            }
            binding.ivRoleIcon.setImageResource(iconRes)
            binding.ivRoleIcon.setBackgroundResource(0) // Quitamos el borde si el icono ya es suficiente

            binding.root.setOnClickListener { onRoleClick(role) }
        }
    }

    class RoleDiffCallback : DiffUtil.ItemCallback<Role>() {
        override fun areItemsTheSame(oldItem: Role, newItem: Role): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Role, newItem: Role): Boolean = oldItem == newItem
    }
}
