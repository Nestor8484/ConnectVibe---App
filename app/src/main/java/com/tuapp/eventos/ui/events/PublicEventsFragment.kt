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

class PublicEventsFragment : Fragment() {

    private var _binding: FragmentEventListBinding? = null
    private val binding get() = _binding!!

    private val eventAdapter = EventAdapter { event ->
        val bundle = Bundle().apply {
            putString("eventId", event.id)
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
        loadData()
        
        binding.fabAddEvent.setOnClickListener {
            findNavController().navigate(R.id.action_global_createEventFragment)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.public_label)
    }

    private fun setupRecyclerView() {
        binding.rvEvents.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = eventAdapter
        }
    }

    private fun loadData() {
        val dummyEvents = listOf(
            Event("2", "Rock Festival", "Public music", Date(), "Madrid", true, "o2"),
            Event("4", "Community Clean-up", "Help the park", Date(), "City Park", true, "o3")
        )
        eventAdapter.submitList(dummyEvents)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
