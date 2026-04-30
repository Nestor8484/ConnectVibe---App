package com.tuapp.eventos.ui.eventdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.tuapp.eventos.databinding.FragmentAddExpenseBinding

class AddExpenseFragment : Fragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val eventId = arguments?.getString("eventId") ?: ""

        setupToolbar()
        setupPayerDropdown()

        binding.btnAddExpense.setOnClickListener {
            val amountStr = binding.etAmount.text.toString()
            val description = binding.etExpenseDescription.text.toString()
            
            if (amountStr.isNotBlank() && description.isNotBlank()) {
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                // In a real flow, we'd get the actual payer ID from the dropdown selection
                // and use a ViewModel to save it to Supabase.
                Toast.makeText(context, "Expense of €$amount saved", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                if (amountStr.isBlank()) binding.etAmount.error = "Amount required"
                if (description.isBlank()) binding.etExpenseDescription.error = "Description required"
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupPayerDropdown() {
        val participants = listOf("John", "Alice", "Bob")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, participants)
        binding.atvPayer.setAdapter(adapter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
