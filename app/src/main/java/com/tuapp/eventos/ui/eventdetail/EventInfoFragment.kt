package com.tuapp.eventos.ui.eventdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.eventos.R
import com.tuapp.eventos.databinding.FragmentEventInfoBinding
import com.tuapp.eventos.domain.model.MemberRole
import com.tuapp.eventos.ui.events.EventViewModel
import com.tuapp.eventos.di.SupabaseModule
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EventInfoFragment : Fragment() {

    private var _binding: FragmentEventInfoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventViewModel by activityViewModels()
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
        observeViewModel()
        
        binding.btnAddParticipant.setOnClickListener {
            viewModel.event.value?.id?.let { eventId ->
                val bundle = Bundle().apply {
                    putString("eventId", eventId)
                }
                findNavController().navigate(R.id.action_global_addParticipantFragment, bundle)
            }
        }

        binding.btnEditEvent.setOnClickListener {
            viewModel.event.value?.let { event ->
                EditEventDialogFragment.newInstance(event).show(childFragmentManager, "EditEventDialogFragment")
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvParticipants.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = participantAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.collectLatest { event ->
                event?.let {
                    binding.tvDetailTitle.text = it.name
                    binding.tvDetailDescription.text = it.description ?: "Sin descripción"
                    
                    val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
                    val isAdmin = it.createdBy == userId
                    binding.btnEditEvent.visibility = if (isAdmin && it.status == "pending") View.VISIBLE else View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.participants.collectLatest { participants ->
                participantAdapter.submitList(participants)
                
                val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val member = participants.find { it.userId == userId }
                    val isAdmin = member?.role == MemberRole.ADMIN
                    val event = viewModel.event.value
                    binding.btnEditEvent.visibility = if (isAdmin && event?.status == "pending") View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
