package com.tuapp.eventos.ui.eventdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.eventos.databinding.FragmentEventParticipantsBinding
import com.tuapp.eventos.domain.model.GroupMember
import com.tuapp.eventos.domain.model.MemberRole
import com.tuapp.eventos.ui.events.MemberAdapter

class EventParticipantsFragment : Fragment() {

    private var _binding: FragmentEventParticipantsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventParticipantsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupRecyclerView()
        loadParticipants()
    }

    private fun setupRecyclerView() {
        binding.rvParticipants.layoutManager = LinearLayoutManager(context)
        // Usamos isAdmin = false para la vista de participantes general
        val adapter = MemberAdapter(isAdmin = false) { _ ->
            // Acción al hacer clic en borrar (no debería ocurrir con isAdmin = false)
        }
        binding.rvParticipants.adapter = adapter
    }

    private fun loadParticipants() {
        val participants = listOf(
            GroupMember("1", "Joan Smith", MemberRole.ADMIN, "joan.smith@example.com"),
            GroupMember("2", "Maria Garcia", MemberRole.MEMBER, "maria.g@example.com"),
            GroupMember("3", "Carlos Lopez", MemberRole.MEMBER, "c.lopez@example.com"),
            GroupMember("4", "Ana Martinez", MemberRole.MEMBER, "ana.mtz@example.com"),
            GroupMember("5", "David Ruiz", MemberRole.MEMBER, "david.r@example.com")
        )
        (binding.rvParticipants.adapter as MemberAdapter).submitList(participants)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
