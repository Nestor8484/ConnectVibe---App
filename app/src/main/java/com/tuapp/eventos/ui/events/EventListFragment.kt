package com.tuapp.eventos.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.eventos.R
import com.tuapp.eventos.databinding.FragmentEventListBinding
import com.tuapp.eventos.domain.model.Event
import java.util.Date

class EventListFragment : Fragment() {

    private var _binding: FragmentEventListBinding? = null
    private val binding get() = _binding!!

    private val eventAdapter = EventAdapter { event ->
        val bundle = Bundle().apply {
            putString("eventId", event.id)
        }
        findNavController().navigate(R.id.action_eventListFragment_to_eventDetailFragment, bundle)
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
        
        setupRecyclerView()
        
        binding.fabAddEvent.setOnClickListener {
            findNavController().navigate(R.id.action_eventListFragment_to_createEventFragment)
        }

        // Dummy data for visual verification
        val dummyEvents = listOf(
            Event("1", "Family Reunion", "Annual BBQ", Date(), "Backyard", false, "owner1"),
            Event("2", "Public Concert", "Rock music fest", Date(), "Main Square", true, "owner2")
        )
        eventAdapter.submitList(dummyEvents)
    }

    private fun setupRecyclerView() {
        binding.rvEvents.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = eventAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
