package com.tuapp.eventos.ui.eventdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.tuapp.eventos.databinding.FragmentAddParticipantBinding

class AddParticipantFragment : Fragment() {

    private var _binding: FragmentAddParticipantBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddParticipantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRoleDropdown()

        binding.btnAdd.setOnClickListener {
            val name = binding.etParticipantName.text.toString()
            if (name.isNotBlank()) {
                Toast.makeText(context, "Participant $name added", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                binding.etParticipantName.error = "Name required"
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRoleDropdown() {
        val roles = listOf("Organizer", "Cook", "Driver", "Guest", "Custom Role...")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roles)
        binding.atvRole.setAdapter(adapter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
