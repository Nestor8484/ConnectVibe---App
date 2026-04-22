package com.tuapp.eventos.ui.eventdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.tuapp.eventos.databinding.FragmentAddTaskBinding

class AddTaskFragment : Fragment() {

    private var _binding: FragmentAddTaskBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupAssigneeDropdown()

        binding.btnCreateTask.setOnClickListener {
            val title = binding.etTaskTitle.text.toString()
            if (title.isNotBlank()) {
                Toast.makeText(context, "Task '$title' created", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                binding.etTaskTitle.error = "Title required"
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupAssigneeDropdown() {
        val participants = listOf("John", "Alice", "Bob", "Unassigned")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, participants)
        binding.atvAssignTo.setAdapter(adapter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
