package com.tuapp.eventos.ui.eventdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.eventos.R
import com.tuapp.eventos.databinding.FragmentEventInfoBinding
import com.tuapp.eventos.domain.model.Participant

class EventInfoFragment : Fragment() {

    private var _binding: FragmentEventInfoBinding? = null
    private val binding get() = _binding!!

    private val participantAdapter = ParticipantAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        
        binding.btnAddParticipant.setOnClickListener {
            findNavController().navigate(R.id.action_eventDetailFragment_to_addParticipantFragment)
        }

        // Dummy participants
        val dummyParticipants = listOf(
            Participant("1", "John Organizer", "Organizer"),
            Participant("2", "Alice Chef", "Cook")
        )
        participantAdapter.submitList(dummyParticipants)
    }

    private fun setupRecyclerView() {
        binding.rvParticipants.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = participantAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
