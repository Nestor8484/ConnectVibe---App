package com.tuapp.eventos.ui.eventdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.activityViewModels
import com.tuapp.eventos.databinding.FragmentAddExpenseBinding
import com.tuapp.eventos.di.SupabaseModule
import com.tuapp.eventos.domain.model.Expense
import com.tuapp.eventos.ui.events.EventViewModel
import io.github.jan.supabase.auth.auth
import java.util.Date

class AddExpenseFragment : Fragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventViewModel by activityViewModels()

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
        setupCategoryDropdown()

        binding.btnAddExpense.setOnClickListener {
            val amountStr = binding.etAmount.text.toString()
            val name = binding.etExpenseName.text.toString()
            val category = binding.atvCategory.text.toString()
            
            if (amountStr.isNotBlank() && name.isNotBlank() && category.isNotBlank()) {
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
                
                val expense = Expense(
                    eventId = eventId,
                    name = name,
                    amount = amount,
                    category = category,
                    payerId = userId,
                    date = Date()
                )
                
                viewModel.addExpense(eventId, expense)
                Toast.makeText(context, "Gasto guardado correctamente", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                if (amountStr.isBlank()) binding.etAmount.error = "Importe requerido"
                if (name.isBlank()) binding.etExpenseName.error = "Nombre requerido"
                if (category.isBlank()) binding.atvCategory.error = "Categoría requerida"
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupCategoryDropdown() {
        val categories = listOf("Comida", "Bebida", "Transporte", "Alquiler", "Decoración", "Otros")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.atvCategory.setAdapter(adapter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
