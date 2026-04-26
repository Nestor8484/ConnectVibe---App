package com.tuapp.eventos.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.eventos.R
import com.tuapp.eventos.databinding.FragmentEventListBinding
import com.tuapp.eventos.di.SupabaseModule
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PublicEventsFragment : Fragment() {

    private var _binding: FragmentEventListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventViewModel by viewModels()

    private val eventAdapter = EventAdapter(
        onEventClick = { event ->
            val bundle = Bundle().apply {
                putString("eventId", event.id)
                putString("eventTitle", event.name)
                putString("eventDescription", event.description)
            }
            findNavController().navigate(R.id.action_global_eventDetailFragment, bundle)
        },
        onJoinClick = { event ->
            showJoinConfirmation(event)
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
        observeViewModel()
        
        viewModel.loadPublicEvents()
        
        binding.fabAddEvent.setOnClickListener {
            findNavController().navigate(R.id.action_global_createEventFragment)
        }

        binding.ivUserProfile.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }
    }

    private fun setupToolbar() {
        binding.tvScreenTitle.text = getString(R.string.public_label)
    }

    private fun setupRecyclerView() {
        binding.rvEvents.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = eventAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.eventsState.collectLatest { state ->
                when (state) {
                    is EventViewModel.EventsState.Loading -> {
                    }
                    is EventViewModel.EventsState.Success -> {
                        eventAdapter.submitList(state.events)
                    }
                    is EventViewModel.EventsState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.joinEventState.collectLatest { state ->
                when (state) {
                    is EventViewModel.JoinEventState.Loading -> {
                    }
                    is EventViewModel.JoinEventState.Success -> {
                        Toast.makeText(context, "Te has unido al evento", Toast.LENGTH_SHORT).show()
                        viewModel.resetJoinState()
                        // Move to joined events or just refresh?
                        findNavController().navigate(R.id.joinedEventsFragment)
                    }
                    is EventViewModel.JoinEventState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetJoinState()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun showJoinConfirmation(event: com.tuapp.eventos.domain.model.Event) {
        AlertDialog.Builder(requireContext())
            .setTitle("Unirse al evento")
            .setMessage("¿Quieres participar en '${event.name}'?")
            .setPositiveButton("Sí") { _, _ ->
                val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
                if (userId != null && event.id != null) {
                    viewModel.joinEvent(event.id, userId)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
