package com.tuapp.eventos.ui.events

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.eventos.databinding.ItemEventBinding
import com.tuapp.eventos.domain.model.Event
import java.text.SimpleDateFormat
import java.util.Locale

class EventAdapter(
    private val onEventClick: (Event) -> Unit
) : ListAdapter<Event, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position), onEventClick)
    }

    class EventViewHolder(private val binding: ItemEventBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun bind(event: Event, onEventClick: (Event) -> Unit) {
            binding.tvEventTitle.text = event.name
            binding.tvEventDate.text = event.startDate?.let { dateFormat.format(it) } ?: "Próximamente"
            binding.tvEventLocation.text = event.visibility.replaceFirstChar { it.uppercase() }

            // Aplicar color e icono
            val color = try {
                android.graphics.Color.parseColor(event.color ?: "#1565C0")
            } catch (e: Exception) {
                android.graphics.Color.parseColor("#1565C0")
            }
            
            binding.ivEventIcon.imageTintList = android.content.res.ColorStateList.valueOf(color)
            binding.root.strokeColor = color
            
            // Mapeo simple de iconos de texto a recursos
            val iconRes = when(event.icon) {
                "Fiesta" -> android.R.drawable.btn_star_big_on
                "Boda" -> android.R.drawable.ic_menu_myplaces
                "Deportes" -> android.R.drawable.ic_menu_directions
                "Viaje" -> android.R.drawable.ic_menu_send
                "Comida" -> android.R.drawable.ic_menu_gallery
                "Trabajo" -> android.R.drawable.ic_dialog_info
                "Educación" -> android.R.drawable.ic_menu_edit
                else -> android.R.drawable.ic_menu_today
            }
            binding.ivEventIcon.setImageResource(iconRes)

            // Show summary if participating
            binding.tvEventParticipantSummary.visibility = if (event.isUserParticipating) android.view.View.VISIBLE else android.view.View.GONE
            binding.tvEventParticipantSummary.setTextColor(color)

            binding.root.setOnClickListener { 
                onEventClick(event) 
            }
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem == newItem
    }
}
