package com.tuapp.eventos.ui.events

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.eventos.R
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
            binding.tvEventTitle.text = event.title
            binding.tvEventDate.text = dateFormat.format(event.date)
            binding.tvEventLocation.text = event.location
            
            if (event.isPublic) {
                binding.tvEventBadge.text = binding.root.context.getString(R.string.public_label)
                binding.tvEventBadge.setBackgroundResource(R.drawable.bg_badge_public)
            } else {
                binding.tvEventBadge.text = binding.root.context.getString(R.string.private_label)
                binding.tvEventBadge.setBackgroundResource(R.drawable.bg_badge_private)
            }

            binding.root.setOnClickListener { onEventClick(event) }
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem == newItem
    }
}
