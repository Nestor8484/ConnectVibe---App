package com.tuapp.eventos.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

    private val eventAdapter = EventAdapter { event ->
        val bundle = Bundle().apply {
            putString("eventId", event.id)
            putString("eventTitle", event.name)
            putString("eventDescription", event.description)
        }
        findNavController().navigate(R.id.action_global_eventDetailFragment, bundle)
    }

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
        
        val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
        viewModel.loadPublicEvents(userId)
        
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
