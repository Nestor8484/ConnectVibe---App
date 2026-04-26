package com.tuapp.eventos.ui.eventdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.tuapp.eventos.R
import com.tuapp.eventos.databinding.FragmentEventDetailBinding
import com.tuapp.eventos.di.SupabaseModule
import com.tuapp.eventos.domain.model.Expense
import com.tuapp.eventos.domain.model.Role
import com.tuapp.eventos.ui.events.EventViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

class EventDetailFragment : Fragment() {

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: EventViewModel by viewModels()
    private var isPieChart = true
    private var currentEventId: String? = null

    private val roleAdapter = RoleAdapter { role ->
        showRoleDetailDialog(role)
    }
    
    private val expenseAdapter = ExpenseAdapter { expense ->
        showExpenseDetailDialog(expense)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentEventId = arguments?.getString("eventId")
        val eventTitle = arguments?.getString("eventTitle") ?: "Evento"
        val eventDesc = arguments?.getString("eventDescription") ?: "Descripción del evento"

        binding.tvEventTitleContainer.text = eventTitle
        binding.tvEventDescription.text = eventDesc

        setupRecyclerViews()
        setupTabs()
        loadData()
        observeViewModel()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.ivUserProfile.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        binding.tvParticipantCount.setOnClickListener {
            val bundle = Bundle().apply {
                putString("eventId", currentEventId)
                putString("eventTitle", eventTitle)
            }
            findNavController().navigate(R.id.eventParticipantsFragment, bundle)
        }

        binding.fabAddExpense.setOnClickListener {
            showCreateExpenseDialog()
        }

        binding.btnToggleChartType.setOnClickListener {
            toggleChart()
        }

        binding.cbParticipateDetail.setOnClickListener {
            val isChecked = binding.cbParticipateDetail.isChecked
            val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
            if (currentEventId != null && userId != null) {
                viewModel.toggleParticipation(currentEventId!!, userId, isChecked)
            }
        }
        
        // Initial data load
        val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
        if (currentEventId != null) {
            viewModel.loadParticipants(currentEventId!!)
            if (userId != null) {
                viewModel.checkParticipation(currentEventId!!, userId)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.joinEventState.collectLatest { state ->
                when (state) {
                    is EventViewModel.JoinEventState.Loading -> {
                        binding.cbParticipateDetail.isEnabled = false
                    }
                    is EventViewModel.JoinEventState.Success -> {
                        binding.cbParticipateDetail.isEnabled = true
                        Toast.makeText(context, "Estado de participación actualizado", Toast.LENGTH_SHORT).show()
                        viewModel.resetJoinState()
                    }
                    is EventViewModel.JoinEventState.Error -> {
                        binding.cbParticipateDetail.isEnabled = true
                        binding.cbParticipateDetail.isChecked = !binding.cbParticipateDetail.isChecked
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetJoinState()
                    }
                    else -> {
                        binding.cbParticipateDetail.isEnabled = true
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isParticipating.collectLatest { isParticipating ->
                binding.cbParticipateDetail.isChecked = isParticipating
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.participants.collectLatest { participants ->
                binding.tvParticipantCount.text = "${participants.size} participantes"
            }
        }
    }

    private fun setupTabs() {
        binding.tabLayout.removeAllTabs()
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.tab_roles)))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.tab_expenses)))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.tab_dashboard)))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateVisibility(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateVisibility(position: Int) {
        binding.rvRoles.visibility = if (position == 0) View.VISIBLE else View.GONE
        binding.rvExpenses.visibility = if (position == 1) View.VISIBLE else View.GONE
        binding.fabAddExpense.visibility = if (position == 1) View.VISIBLE else View.GONE
        binding.layoutDashboard.visibility = if (position == 2) View.VISIBLE else View.GONE
    }

    private fun toggleChart() {
        isPieChart = !isPieChart
        if (isPieChart) {
            binding.viewPieChart.visibility = View.VISIBLE
            binding.viewBarChart.visibility = View.GONE
            binding.btnToggleChartType.text = getString(R.string.view_bar_chart)
        } else {
            binding.viewPieChart.visibility = View.GONE
            binding.viewBarChart.visibility = View.VISIBLE
            binding.btnToggleChartType.text = getString(R.string.view_pie_chart)
        }
    }

    private fun showRoleDetailDialog(role: Role) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_role_detail, null)
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_ConnectVibe_Dialog)
            .setView(dialogView)
            .create()

        val tvName = dialogView.findViewById<TextView>(R.id.tvDialogRoleName)
        val tvDesc = dialogView.findViewById<TextView>(R.id.tvDialogRoleDescription)
        val tvTasks = dialogView.findViewById<TextView>(R.id.tvDialogTasks)
        val btnAssign = dialogView.findViewById<MaterialButton>(R.id.btnAssign)
        val btnClose = dialogView.findViewById<MaterialButton>(R.id.btnClose)

        tvName.text = role.name
        tvDesc.text = role.description
        
        val tasksText = when (role.name) {
            "Logística" -> "• Alquilar furgoneta\n• Recoger sillas y mesas\n• Montar escenario"
            "Catering" -> "• Comprar bebidas\n• Encargar canapés\n• Organizar mesa de comida"
            "Música" -> "• Preparar lista Spotify\n• Llevar altavoces Bluetooth\n• Gestionar peticiones"
            else -> "• Tarea de apoyo 1\n• Tarea de apoyo 2"
        }
        tvTasks.text = tasksText

        btnAssign.setOnClickListener { dialog.dismiss() }
        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showExpenseDetailDialog(expense: Expense) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_expense_detail, null)
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_ConnectVibe_Dialog)
            .setView(dialogView)
            .create()

        val tvName = dialogView.findViewById<TextView>(R.id.tvDialogExpenseName)
        val tvAmount = dialogView.findViewById<TextView>(R.id.tvDialogExpenseAmount)
        val tvType = dialogView.findViewById<TextView>(R.id.tvDialogExpenseType)
        val btnClose = dialogView.findViewById<MaterialButton>(R.id.btnCloseExpense)

        tvName.text = expense.description
        tvAmount.text = String.format(Locale.getDefault(), "%.2f€", expense.amount)
        tvType.text = "Categoría: Comida y Bebida"

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showCreateExpenseDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_expense, null)
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_ConnectVibe_Dialog)
            .setView(dialogView)
            .create()

        val categories = arrayOf("Comida", "Bebida", "Transporte", "Alojamiento", "Entretenimiento", "Otros")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        val autoCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.etExpenseCategory)
        autoCategory.setAdapter(adapter)

        val btnCreate = dialogView.findViewById<MaterialButton>(R.id.btnCreateExpense)
        btnCreate.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupRecyclerViews() {
        binding.rvRoles.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = roleAdapter
        }
        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = expenseAdapter
        }
    }

    private fun loadData() {
        // Placeholder roles for now
        val dummyRoles = listOf(
            Role(name = "Logística", description = "Encargado de transporte y materiales"),
            Role(name = "Catering", description = "Gestión de comida y bebida"),
            Role(name = "Música", description = "Encargado de la playlist y sonido")
        )
        roleAdapter.submitList(dummyRoles)

        val dummyExpenses = listOf(
            Expense("e1", "Bebidas Fiesta", 150.0, "u1", Date()),
            Expense("e2", "Alquiler Sonido", 85.50, "u1", Date()),
            Expense("e3", "Decoración Local", 40.0, "u1", Date())
        )
        expenseAdapter.submitList(dummyExpenses)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
