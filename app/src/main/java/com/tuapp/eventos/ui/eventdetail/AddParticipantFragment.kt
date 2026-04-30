package com.tuapp.eventos.ui.eventdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.tuapp.eventos.databinding.FragmentAddParticipantBinding
import com.tuapp.eventos.ui.events.EventViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddParticipantFragment : Fragment() {

    private var _binding: FragmentAddParticipantBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventViewModel by activityViewModels()
    private var eventId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddParticipantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        eventId = arguments?.getString("eventId") ?: ""

        setupToolbar()
        observeViewModel()

        binding.btnAdd.setOnClickListener {
            val name = binding.etParticipantName.text.toString()
            if (name.isNotBlank()) {
                // In a real app, we would search for a user first.
                // For now, we'll use a placeholder logic or rely on participation toggle.
                Toast.makeText(context, "Search for user logic pending. For now, join/leave is used in the UI.", Toast.LENGTH_SHORT).show()
                // findNavController().popBackStack()
            } else {
                binding.etParticipantName.error = "Name required"
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.roles.collectLatest { roles ->
                val roleNames = roles.map { it.name }.toMutableList()
                if (roleNames.isEmpty()) {
                    roleNames.addAll(listOf("Organizer", "Guest"))
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roleNames)
                binding.atvRole.setAdapter(adapter)
            }
        }
        
        if (eventId.isNotEmpty()) {
            viewModel.loadRoles(eventId)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
