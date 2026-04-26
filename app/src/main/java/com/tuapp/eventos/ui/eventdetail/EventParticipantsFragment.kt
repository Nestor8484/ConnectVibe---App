package com.tuapp.eventos.ui.eventdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.eventos.databinding.FragmentEventParticipantsBinding
import com.tuapp.eventos.ui.events.EventViewModel
import com.tuapp.eventos.ui.events.MemberAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EventParticipantsFragment : Fragment() {

    private var _binding: FragmentEventParticipantsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventViewModel by viewModels()
    private val memberAdapter = MemberAdapter(isAdmin = false) { _ -> }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventParticipantsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val eventId = arguments?.getString("eventId")
        val eventTitle = arguments?.getString("eventTitle")

        if (eventTitle != null) {
            binding.tvTitle.text = eventTitle
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupRecyclerView()
        observeViewModel()

        if (eventId != null) {
            viewModel.loadParticipants(eventId)
        }
    }

    private fun setupRecyclerView() {
        binding.rvParticipants.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = memberAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.participants.collectLatest { participants ->
                // Mapeamos los miembros del dominio al formato que espera el MemberAdapter (data + profile)
                val mappedParticipants = participants.map { domainMember ->
                    com.tuapp.eventos.data.model.GroupMember(
                        group_id = "", // No relevante aquí
                        user_id = domainMember.userId,
                        is_admin = domainMember.role == com.tuapp.eventos.domain.model.MemberRole.ADMIN
                    ) to com.tuapp.eventos.data.model.Profile(
                        id = domainMember.userId,
                        full_name = domainMember.userName,
                        username = domainMember.email
                    )
                }
                memberAdapter.submitList(mappedParticipants)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
