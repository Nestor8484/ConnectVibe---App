package com.tuapp.eventos.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.eventos.R
import com.tuapp.eventos.databinding.FragmentEventListBinding
import com.tuapp.eventos.di.SupabaseModule
import com.tuapp.eventos.ui.profile.NotificationViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class JoinedEventsFragment : Fragment() {

    private var _binding: FragmentEventListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()
    private var allEvents: List<com.tuapp.eventos.domain.model.Event> = emptyList()

    private val eventAdapter = EventAdapter(
        onEventClick = { event ->
            val bundle = Bundle().apply {
                putString("eventId", event.id)
                putString("eventTitle", event.name)
                putString("eventDescription", event.description)
            }
            findNavController().navigate(R.id.action_global_eventDetailFragment, bundle)
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupRecyclerView()
        setupFilters()
        observeViewModel()
        
        val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
        if (userId != null) {
            viewModel.loadJoinedEvents(userId)
            notificationViewModel.loadNotifications(userId)
        }

        binding.fabAddEvent.setOnClickListener {
            findNavController().navigate(R.id.action_global_createEventFragment)
        }

        binding.ivUserProfile.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }
    }

    private fun setupToolbar() {
        binding.tvScreenTitle.text = getString(R.string.joined_label)
    }

    private fun setupRecyclerView() {
        binding.rvEvents.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = eventAdapter
        }
    }

    private fun setupFilters() {
        val options = arrayOf("Todos", "Pendientes", "En curso", "Finalizados")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        binding.actvEventFilter.setAdapter(adapter)
        
        // Mantener la selección actual o poner Pendientes por defecto
        val currentText = binding.actvEventFilter.text.toString()
        if (currentText.isEmpty() || !options.contains(currentText)) {
            binding.actvEventFilter.setText(options[1], false)
        }

        binding.actvEventFilter.setOnItemClickListener { _, _, position, _ ->
            filterEvents(options[position])
        }
    }

    private fun filterEvents(filter: String) {
        val filtered = when (filter) {
            "Pendientes" -> allEvents.filter { it.status == "pending" }
            "En curso" -> allEvents.filter { it.status == "started" }
            "Finalizados" -> allEvents.filter { it.status == "finished" }
            else -> allEvents
        }
        eventAdapter.submitList(filtered)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.eventsState.collectLatest { state ->
                when (state) {
                    is EventViewModel.EventsState.Loading -> {
                    }
                    is EventViewModel.EventsState.Success -> {
                        allEvents = state.events
                        filterEvents(binding.actvEventFilter.text.toString())
                    }
                    is EventViewModel.EventsState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            notificationViewModel.hasPendingNotifications.collectLatest { hasPending ->
                binding.vNotificationBadge.visibility = if (hasPending) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
