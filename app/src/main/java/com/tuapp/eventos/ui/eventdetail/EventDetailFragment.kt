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
import com.tuapp.eventos.domain.model.MemberRole
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
    private var isUserAdmin = false

    private val roleAdapter = RoleAdapter(
        onRoleClick = { role -> 
            if (isUserAdmin && viewModel.event.value?.status == "pending") {
                val addRoleFragment = AddRoleFragment.newInstance(currentEventId ?: "", role)
                addRoleFragment.show(childFragmentManager, "AddRoleFragment")
            } else {
                showRoleDetailDialog(role) 
            }
        },
        onDeleteClick = { role -> showDeleteRoleDialog(role) }
    )
    
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

        binding.fabAddRole.setOnClickListener {
            currentEventId?.let { id ->
                AddRoleFragment.newInstance(id).show(childFragmentManager, "AddRoleFragment")
            }
        }

        binding.btnToggleChartType.setOnClickListener {
            toggleChart()
        }

        binding.fabEventAction.setOnClickListener {
            handleStatusAction()
        }

        binding.cbParticipateDetail.setOnClickListener {
            val isChecked = binding.cbParticipateDetail.isChecked
            val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
            val currentEvent = viewModel.event.value
            val isOwner = currentEvent?.createdBy == userId
            
            if (currentEventId != null && userId != null) {
                viewModel.toggleParticipation(currentEventId!!, userId, isChecked, isOwner)
            }
        }
        
        // Initial data load
        val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
        if (currentEventId != null) {
            viewModel.loadEvent(currentEventId!!)
            viewModel.loadParticipants(currentEventId!!)
            viewModel.loadRoles(currentEventId!!)
            if (userId != null) {
                viewModel.checkParticipation(currentEventId!!, userId)
                checkAdminStatus(userId)
            }
        }
    }

    private fun checkAdminStatus(userId: String) {
        // Logic to check if user is admin of the event
        // For now, let's assume if they created the event or based on participation
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.participants.collectLatest { participants ->
                val member = participants.find { it.userId == userId }
                isUserAdmin = member?.role == MemberRole.ADMIN
                updateAdminUi()
            }
        }
    }

    private fun updateAdminUi() {
        val currentEvent = viewModel.event.value
        val status = currentEvent?.status ?: "pending"
        
        if (binding.tabLayout.selectedTabPosition == 0) {
            binding.fabAddRole.visibility = if (isUserAdmin && status == "pending") View.VISIBLE else View.GONE
        }
        
        if (isUserAdmin) {
            binding.fabEventAction.visibility = View.VISIBLE
            when (status) {
                "pending" -> {
                    binding.fabEventAction.setImageResource(android.R.drawable.ic_media_play)
                    binding.fabEventAction.contentDescription = "Iniciar Evento"
                }
                "started" -> {
                    binding.fabEventAction.setImageResource(android.R.drawable.ic_menu_save)
                    binding.fabEventAction.contentDescription = "Finalizar Evento"
                }
                "finished" -> {
                    binding.fabEventAction.visibility = View.GONE
                }
            }
        } else {
            binding.fabEventAction.visibility = View.GONE
        }

        roleAdapter.setAdminStatus(isUserAdmin && status == "pending")
        
        binding.cbParticipateDetail.isEnabled = !isUserAdmin
        binding.cbParticipateDetail.alpha = if (isUserAdmin) 0.6f else 1.0f
    }

    private fun handleStatusAction() {
        val currentEvent = viewModel.event.value ?: return
        val status = currentEvent.status

        when (status) {
            "pending" -> checkRolesAndStart()
            "started" -> showFinishConfirmation()
        }
    }

    private fun checkRolesAndStart() {
        val roles = viewModel.roles.value
        val members = viewModel.roleMembers.value
        
        val rolesIncomplete = roles.filter { role ->
            val minNeeded = role.minPeople ?: if (role.isMandatory) 1 else 0
            val currentCount = members.count { it.roleId == role.id }
            currentCount < minNeeded
        }

        if (rolesIncomplete.isNotEmpty()) {
            MaterialAlertDialogBuilder(requireContext(), R.style.Theme_ConnectVibe_Dialog)
                .setTitle("Roles incompletos")
                .setMessage("Hay roles obligatorios o con un mínimo de personas que aún no están cubiertos. ¿Quieres que el sistema asigne automáticamente a los participantes disponibles?")
                .setNegativeButton("No, empezar así") { _, _ ->
                    currentEventId?.let { viewModel.startEvent(it, autoAssign = false) }
                }
                .setNeutralButton("Cancelar", null)
                .setPositiveButton("Auto-asignar y empezar") { _, _ ->
                    currentEventId?.let { viewModel.startEvent(it, autoAssign = true) }
                }
                .show()
                .window?.setBackgroundDrawableResource(android.R.color.transparent)
        } else {
            currentEventId?.let { viewModel.startEvent(it, autoAssign = false) }
        }
    }

    private fun showFinishConfirmation() {
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_ConnectVibe_Dialog)
            .setTitle("Finalizar Evento")
            .setMessage("¿Estás seguro de que quieres finalizar el evento? Esta acción es irreversible y moverá el evento a tu historial. Ya no se podrán realizar más cambios.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Finalizar") { _, _ ->
                currentEventId?.let { viewModel.finishEvent(it) }
            }
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.joinEventState.collectLatest { state ->
                when (state) {
                    is EventViewModel.JoinEventState.Loading -> {
                        binding.cbParticipateDetail.isEnabled = false
                    }
                    is EventViewModel.JoinEventState.Success -> {
                        binding.cbParticipateDetail.isEnabled = !isUserAdmin
                        Toast.makeText(context, "Estado de participación actualizado", Toast.LENGTH_SHORT).show()
                        viewModel.resetJoinState()
                    }
                    is EventViewModel.JoinEventState.Error -> {
                        binding.cbParticipateDetail.isEnabled = !isUserAdmin
                        binding.cbParticipateDetail.isChecked = !binding.cbParticipateDetail.isChecked
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetJoinState()
                    }
                    else -> {
                        binding.cbParticipateDetail.isEnabled = !isUserAdmin
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
                // Re-check admin status when participants list is updated
                val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val member = participants.find { it.userId == userId }
                    isUserAdmin = member?.role == MemberRole.ADMIN
                    updateAdminUi()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.collectLatest { event ->
                event?.let {
                    roleAdapter.setEventStatus(it.status)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.roles.collectLatest { roles ->
                roleAdapter.submitList(roles)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.roleMembers.collectLatest { members ->
                roleAdapter.setRoleMembers(members)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.roleOpState.collectLatest { state ->
                if (state is EventViewModel.RoleOpState.Success) {
                    viewModel.resetRoleOpState()
                } else if (state is EventViewModel.RoleOpState.Error) {
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetRoleOpState()
                }
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
        binding.fabAddRole.visibility = if (position == 0 && isUserAdmin) View.VISIBLE else View.GONE
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
        val tvStatus = dialogView.findViewById<TextView>(R.id.tvRoleStatus)
        val tvMin = dialogView.findViewById<TextView>(R.id.tvMinPeople)
        val tvMax = dialogView.findViewById<TextView>(R.id.tvMaxPeople)
        val btnAssign = dialogView.findViewById<MaterialButton>(R.id.btnAssign)
        val btnEdit = dialogView.findViewById<MaterialButton>(R.id.btnEditRole)
        val btnClose = dialogView.findViewById<MaterialButton>(R.id.btnClose)
        val tvLabelMembers = dialogView.findViewById<TextView>(R.id.tvLabelMembers)
        val tvAssignedMembers = dialogView.findViewById<TextView>(R.id.tvAssignedMembers)

        tvName.text = role.name
        tvDesc.text = role.description
        tvStatus.visibility = if (role.isMandatory) View.VISIBLE else View.GONE
        tvMin.text = (role.minPeople ?: 0).toString()
        tvMax.text = (role.maxPeople ?: "∞").toString()
        
        // Mostrar miembros asignados
        val membersInRole = viewModel.roleMembers.value.filter { it.roleId == role.id }
        if (membersInRole.isNotEmpty()) {
            tvLabelMembers.visibility = View.VISIBLE
            tvAssignedMembers.visibility = View.VISIBLE
            
            val memberNames = membersInRole.map { member ->
                viewModel.participants.value.find { it.userId == member.userId }?.userName ?: "Usuario"
            }.joinToString(", ")
            
            tvAssignedMembers.text = memberNames
        } else {
            tvLabelMembers.visibility = View.VISIBLE
            tvAssignedMembers.text = "Nadie asignado aún"
        }

        val currentUserId = SupabaseModule.client.auth.currentUserOrNull()?.id
        val isUserAssigned = membersInRole.any { it.userId == currentUserId }
        
        btnAssign.text = if (isUserAssigned) "Desasignarme" else "Asignarme"
        btnAssign.setIconResource(if (isUserAssigned) android.R.drawable.ic_menu_close_clear_cancel else android.R.drawable.ic_input_add)

        btnAssign.setOnClickListener { 
            if (currentUserId != null && currentEventId != null) {
                viewModel.toggleRoleAssignment(role.id!!, currentUserId, currentEventId!!, !isUserAssigned)
                dialog.dismiss()
            }
        }
        
        val status = viewModel.event.value?.status ?: "pending"
        btnEdit.visibility = if (isUserAdmin && status == "pending") View.VISIBLE else View.GONE
        btnEdit.setOnClickListener {
            dialog.dismiss()
            currentEventId?.let { id ->
                AddRoleFragment.newInstance(id, role).show(childFragmentManager, "AddRoleFragment")
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()

        // Forzar ancho al 90%
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showDeleteRoleDialog(role: Role) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Rol")
            .setMessage("¿Estás seguro de que quieres eliminar el rol \"${role.name}\"?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                currentEventId?.let { eventId ->
                    role.id?.let { roleId ->
                        viewModel.deleteRole(roleId, eventId)
                    }
                }
            }
            .show()
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

        // Forzar ancho al 90%
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
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
        // Data is now loaded from ViewModel via observeViewModel
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
