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
import com.tuapp.eventos.di.SupabaseModule
import com.tuapp.eventos.domain.model.MemberRole
import com.tuapp.eventos.ui.events.EventViewModel
import com.tuapp.eventos.ui.events.MemberAdapter
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class EventParticipantsFragment : Fragment() {

    private var _binding: FragmentEventParticipantsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventViewModel by viewModels()
    private val memberAdapter = MemberAdapter(
        isUserAdmin = false,
        onPromoteAdmin = { userId, isAdmin ->
            arguments?.getString("eventId")?.let { eventId ->
                viewModel.updateParticipantRole(eventId, userId, isAdmin)
            }
        },
        onRemoveMember = { userId ->
            arguments?.getString("eventId")?.let { eventId ->
                viewModel.removeParticipant(eventId, userId)
            }
        }
    )

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
            viewModel.loadEvent(eventId)
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
            combine(viewModel.event, viewModel.participants) { event, participants ->
                event to participants
            }.collectLatest { (event, participants) ->
                val currentUserId = SupabaseModule.client.auth.currentUserOrNull()?.id
                val isCreator = event?.createdBy == currentUserId
                
                // El usuario es admin si es el creador o si tiene rol de ADMIN en la lista
                val currentUserMember = participants.find { it.userId == currentUserId }
                val isUserAdmin = isCreator || currentUserMember?.role == MemberRole.ADMIN
                
                memberAdapter.setAdminStatus(isUserAdmin, isCreator, event?.createdBy)

                // Mapeamos los miembros del dominio al formato que espera el MemberAdapter (data + profile)
                val mappedParticipants = participants.map { domainMember ->
                    com.tuapp.eventos.data.model.GroupMember(
                        group_id = "", // No relevante aquí
                        user_id = domainMember.userId,
                        is_admin = domainMember.role == MemberRole.ADMIN
                    ) to com.tuapp.eventos.data.model.Profile(
                        id = domainMember.userId,
                        full_name = domainMember.userName,
                        username = domainMember.email.removePrefix("@")
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
