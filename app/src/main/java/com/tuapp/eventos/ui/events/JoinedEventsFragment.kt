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

class JoinedEventsFragment : Fragment() {

    private var _binding: FragmentEventListBinding? = null
    private val binding get() = _binding!!

    private val eventAdapter = EventAdapter { event ->
        val bundle = Bundle().apply {
            putString("eventId", event.id)
            putString("eventTitle", event.title)
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
        loadData()
        
        binding.fabAddEvent.visibility = View.GONE

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

    private fun loadData() {
        // Here you would normally fetch events the user is participating in
        val dummyEvents = listOf(
            Event("1", "Family BBQ", "Private fun", Date(), "Valencia", false, "o1"),
            Event("2", "Rock Festival", "Public music", Date(), "Madrid", true, "o2")
        )
        eventAdapter.submitList(dummyEvents)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
