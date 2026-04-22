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

        setupToolbar()
        setupPayerDropdown()

        binding.btnAddExpense.setOnClickListener {
            val amount = binding.etAmount.text.toString()
            if (amount.isNotBlank()) {
                Toast.makeText(context, "Expense of €$amount saved", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                binding.etAmount.error = "Amount required"
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
