package com.tuapp.eventos.ui.events

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.eventos.databinding.ItemEventStatusSmallBinding
import com.tuapp.eventos.domain.model.Event
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

class EventStatusAdapter : ListAdapter<Event, EventStatusAdapter.ViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEventStatusSmallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemEventStatusSmallBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(event: Event) {
            binding.tvEventName.text = event.name
            
            binding.tvEventStatus.text = when(event.status) {
                "pending" -> "Pendiente"
                "started" -> "En curso"
                "finished" -> "Finalizado"
                else -> event.status.uppercase()
            }

            val color = try {
                Color.parseColor(event.color ?: "#1565C0")
            } catch (e: Exception) {
                Color.parseColor("#1565C0")
            }

            binding.tvEventStatus.setTextColor(color)
            binding.tvEventName.setTextColor(color)
            
            val bg = binding.tvEventStatus.background.mutate() as? GradientDrawable
            bg?.setStroke(2, color)
            binding.tvEventStatus.backgroundTintList = ColorStateList.valueOf(color).withAlpha(30)
            
            binding.root.strokeColor = color
            val cardBg = (0.1 * 255).toInt() shl 24 or (color and 0x00FFFFFF)
            binding.root.setCardBackgroundColor(cardBg)
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem == newItem
    }
}