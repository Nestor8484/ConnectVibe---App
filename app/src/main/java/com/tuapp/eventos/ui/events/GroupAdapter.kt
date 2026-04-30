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
            binding.tvEventLocation.text = group.description ?: "Sin descripción"
            binding.tvEventDate.visibility = android.view.View.GONE
            binding.llParticipantSummary.visibility = android.view.View.GONE
            
            // Aplicar icono y color dinámico (estilo Roles/Eventos)
            val context = binding.root.context
            val iconRes = when (group.icon) {
                "Familia" -> com.tuapp.eventos.R.drawable.ic_groups 
                "Trabajo" -> android.R.drawable.ic_menu_agenda
                "Deportes" -> android.R.drawable.ic_menu_directions
                "Estudios" -> android.R.drawable.ic_menu_edit
                "Viajes" -> android.R.drawable.ic_menu_compass
                "Hobby" -> android.R.drawable.ic_menu_gallery
                "ic_groups", "Grupo de Amigos" -> com.tuapp.eventos.R.drawable.ic_groups
                else -> com.tuapp.eventos.R.drawable.ic_groups
            }
            binding.ivEventIcon.setImageResource(iconRes)

            group.color?.let { colorStr ->
                try {
                    val colorInt = android.graphics.Color.parseColor(colorStr)
                    val alphaColor = (0.15 * 255).toInt() shl 24 or (colorInt and 0x00FFFFFF)
                    val density = context.resources.displayMetrics.density

                    // Estilo de la Card
                    binding.root.setCardBackgroundColor(alphaColor)
                    binding.root.strokeColor = colorInt
                    binding.root.strokeWidth = (2 * density).toInt()

                    // Icono y Texto
                    binding.ivEventIcon.imageTintList = android.content.res.ColorStateList.valueOf(colorInt)
                    binding.ivEventIcon.background.mutate().setTint(android.graphics.Color.TRANSPARENT) // Quitar fondo circular si existe
                    binding.tvEventTitle.setTextColor(colorInt)
                } catch (e: Exception) {
                    // Fallback a colores por defecto
                }
            }

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
