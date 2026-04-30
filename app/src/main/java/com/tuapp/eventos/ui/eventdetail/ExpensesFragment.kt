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
import com.tuapp.eventos.databinding.FragmentExpensesBinding
import com.tuapp.eventos.ui.events.EventViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ExpensesFragment : Fragment() {
    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventViewModel by activityViewModels()
    private val expenseAdapter = ExpenseAdapter { expense ->
        // Handle expense click
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
        
        binding.fabAddExpense.setOnClickListener {
            val eventId = arguments?.getString("eventId")
            val bundle = Bundle().apply {
                putString("eventId", eventId)
            }
            findNavController().navigate(R.id.action_global_addExpenseFragment, bundle)
        }
    }

    private fun setupRecyclerView() {
        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = expenseAdapter
        }
    }

    private fun observeViewModel() {
        val eventId = arguments?.getString("eventId") ?: return
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.expenses.collectLatest { expenses ->
                expenseAdapter.submitList(expenses)
            }
        }
        
        viewModel.loadExpenses(eventId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
