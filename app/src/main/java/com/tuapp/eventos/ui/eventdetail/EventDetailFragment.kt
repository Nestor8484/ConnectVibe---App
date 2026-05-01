package com.tuapp.eventos.ui.eventdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
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
import com.tuapp.eventos.domain.model.Event
import com.tuapp.eventos.domain.model.EventRoleMember
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

    private val participantAdapter = ParticipantAdapter()

    private val roleAdapter = RoleAdapter(
        onRoleClick = { role -> 
            showRoleDetailDialog(role)
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
        binding.tvInfoDescription.text = eventDesc

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
            binding.tabLayout.getTabAt(3)?.select()
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

        binding.btnStatusAction.setOnClickListener {
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
        val selectedTab = binding.tabLayout.selectedTabPosition
        
        binding.fabAddRole.visibility = if (isUserAdmin && status == "pending" && selectedTab == 0) View.VISIBLE else View.GONE
        binding.fabAddExpense.visibility = if (selectedTab == 1) View.VISIBLE else View.GONE
        
        // Habilitar/Deshabilitar botón de editar evento
        // Buscamos el botón en el layout de información
        val btnEditEvent = binding.layoutInfo.findViewById<MaterialButton>(R.id.btnEditEvent)
        btnEditEvent?.visibility = if (isUserAdmin && status == "pending") View.VISIBLE else View.GONE
        btnEditEvent?.setOnClickListener {
            currentEvent?.let { event ->
                EditEventDialogFragment.newInstance(event).show(childFragmentManager, "EditEventDialogFragment")
            }
        }

        val btnDeleteEvent = binding.layoutInfo.findViewById<MaterialButton>(R.id.btnDeleteEvent)
        btnDeleteEvent?.visibility = if (isUserAdmin && status == "pending") View.VISIBLE else View.GONE
        btnDeleteEvent?.setOnClickListener {
            showDeleteEventConfirmation()
        }

        if (isUserAdmin) {
            binding.btnStatusAction.visibility = if (selectedTab == 3) View.VISIBLE else View.GONE
            when (status) {
                "pending" -> {
                    binding.btnStatusAction.text = "Iniciar Evento"
                }
                "started" -> {
                    binding.btnStatusAction.text = "Finalizar Evento"
                }
                "finished" -> {
                    binding.btnStatusAction.visibility = View.GONE
                }
            }
        } else {
            binding.btnStatusAction.visibility = View.GONE
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
            MaterialAlertDialogBuilder(requireContext())
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
        } else {
            currentEventId?.let { viewModel.startEvent(it, autoAssign = false) }
        }
    }

    private fun showFinishConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Finalizar Evento")
            .setMessage("¿Estás seguro de que quieres finalizar el evento? Esta acción es irreversible y moverá el evento a tu historial. Ya no se podrán realizar más cambios.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Finalizar") { _, _ ->
                currentEventId?.let { viewModel.finishEvent(it) }
            }
            .show()
    }

    private fun showDeleteEventConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Evento")
            .setMessage("¿Estás seguro de que quieres eliminar permanentemente este evento y todos sus datos? Esta acción no se puede deshacer.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                currentEventId?.let { viewModel.deleteEvent(it) }
            }
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.createEventState.collectLatest { state ->
                when (state) {
                    is EventViewModel.CreateEventState.Loading -> {
                        // Opcional: Mostrar progreso
                    }
                    is EventViewModel.CreateEventState.Success -> {
                        // Si el evento ya no existe en el StateFlow, es que fue eliminado
                        if (viewModel.event.value == null && currentEventId != null) {
                            Toast.makeText(context, "Evento eliminado", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        } else {
                            // Si fue una actualización, el StateFlow de event ya se habrá actualizado
                            Toast.makeText(context, "Evento actualizado", Toast.LENGTH_SHORT).show()
                        }
                        viewModel.resetCreateState()
                    }
                    is EventViewModel.CreateEventState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetCreateState()
                    }
                    else -> {}
                }
            }
        }

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
                participantAdapter.submitList(participants)
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
                    
                    // Actualizar UI con datos reales del evento
                    binding.tvEventTitleContainer.text = it.name
                    binding.tvInfoDescription.text = it.description ?: "Sin descripción"
                    
                    val tvLocation = binding.layoutInfo.findViewById<TextView>(R.id.tvInfoLocation)
                    tvLocation?.text = it.location ?: "Ubicación no especificada"
                    
                    // Aplicar color del evento dinámicamente SOLO en esta pantalla
                    it.color?.let { colorStr ->
                        try {
                            val colorInt = colorStr.toColorInt()
                            
                            // Calcular colores derivados (estilo roles: fondo suave + borde fuerte)
                            // Aumentamos ligeramente la opacidad del fondo (15%) para que se vea más
                            val alphaColor = (0.15 * 255).toInt() shl 24 or (colorInt and 0x00FFFFFF)
                            val density = resources.displayMetrics.density

                            // 1. Cabecera: El contenedor del título con fondo suave y borde más grueso
                            val titleBg = binding.tvEventTitleContainer.background.mutate() as? android.graphics.drawable.GradientDrawable
                            titleBg?.let {
                                it.setColor(alphaColor)
                                it.setStroke((3 * density).toInt(), colorInt) // Aumentado a 3dp
                            }
                            binding.tvEventTitleContainer.setTextColor(colorInt)
                            
                            // 2. Elementos de interacción superior
                            binding.cbParticipateDetail.buttonTintList = android.content.res.ColorStateList.valueOf(colorInt)
                            binding.cbParticipateDetail.setTextColor(colorInt)
                            
                            binding.tvParticipantCount.setTextColor(colorInt)
                            val participantContainer = binding.tvParticipantCount.parent as? ViewGroup
                            val partBg = participantContainer?.background?.mutate() as? android.graphics.drawable.GradientDrawable
                            partBg?.let {
                                it.setColor(alphaColor)
                                it.setStroke((2 * density).toInt(), colorInt) // Aumentado a 2dp
                            }
                            val groupIcon = participantContainer?.getChildAt(0) as? ImageView
                            groupIcon?.imageTintList = android.content.res.ColorStateList.valueOf(colorInt)

                            // 3. Ajuste al contenedor principal de contenido
                            val contentBox = binding.tabLayout.parent as? ViewGroup
                            val contentBg = contentBox?.background?.mutate() as? android.graphics.drawable.GradientDrawable
                            contentBg?.let {
                                val borderAlphaColor = (0.3 * 255).toInt() shl 24 or (colorInt and 0x00FFFFFF)
                                it.setColor(android.graphics.Color.TRANSPARENT)
                                it.setStroke((2 * density).toInt(), borderAlphaColor) // Borde de la caja más visible
                            }

                            // 4. Pestañas (Tabs)
                            binding.tabLayout.setSelectedTabIndicatorColor(colorInt)
                            binding.tabLayout.setTabTextColors(
                                ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant),
                                colorInt
                            )

                            // 4. Títulos de secciones específicos de la pestaña Información
                            // Usamos findViewWithTag o simplemente IDs directos para no romper nada
                            view?.findViewById<TextView>(R.id.btnEditEvent)?.let { it.parent as? RelativeLayout }?.let { rel ->
                                (rel.getChildAt(0) as? TextView)?.setTextColor(colorInt)
                            }
                            
                            // Etiquetas de campos (Descripción, Ubicación, Participantes)
                            // Buscamos los TextViews que actúan como etiquetas por su texto o posición
                            binding.layoutInfo.findViewById<TextView>(R.id.tvInfoDescription)?.let { 
                                // El TextView que está justo encima de la descripción es la etiqueta "Descripción"
                                val parent = it.parent as? ViewGroup
                                val index = parent?.indexOfChild(it) ?: -1
                                if (index > 0) (parent?.getChildAt(index - 1) as? TextView)?.setTextColor(colorInt)
                                
                                // Lo mismo para Ubicación
                                val locView = binding.layoutInfo.findViewById<TextView>(R.id.tvInfoLocation)
                                val locIndex = parent?.indexOfChild(locView) ?: -1
                                if (locIndex > 0) (parent?.getChildAt(locIndex - 1) as? TextView)?.setTextColor(colorInt)
                                
                                // Y para Participantes
                                val partView = binding.layoutInfo.findViewById<View>(R.id.rvParticipantsDetail)
                                val partIndex = parent?.indexOfChild(partView) ?: -1
                                if (partIndex > 0) (parent?.getChildAt(partIndex - 1) as? TextView)?.setTextColor(colorInt)
                            }

                            // 5. Botones y FABs - Solo los de esta pantalla
                            binding.btnStatusAction.backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)
                            binding.btnEditEvent.iconTint = android.content.res.ColorStateList.valueOf(colorInt)
                            binding.fabAddRole.backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)
                            binding.fabAddExpense.backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)

                        } catch (e: Exception) {
                            android.util.Log.e("EventDetail", "Error applying event color: ${e.message}")
                        }
                    }
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

        // Observar gastos para el Dashboard
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.expenses.collectLatest { expenses ->
                updateDashboardExpenses(expenses)
                expenseAdapter.submitList(expenses)
            }
        }

        // Observar roles y miembros para el Dashboard
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(viewModel.roles, viewModel.roleMembers) { roles, members ->
                roles to members
            }.collectLatest { (roles, members) ->
                updateDashboardRoles(roles, members)
            }
        }

        // Observar tareas para el Dashboard
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tasks.collectLatest { tasks ->
                updateTaskProgress(tasks)
            }
        }
    }

    private fun updateTaskProgress(tasks: List<com.tuapp.eventos.domain.model.EventTask>) {
        if (tasks.isEmpty()) {
            binding.taskProgressBar.progress = 0
            binding.tvTaskProgress.text = "No hay tareas asignadas"
            return
        }

        val completedTasks = tasks.count { it.isCompleted }
        val progress = (completedTasks.toFloat() / tasks.size * 100).toInt()

        binding.taskProgressBar.progress = progress
        binding.tvTaskProgress.text = "$progress% de las tareas del evento completadas ($completedTasks/${tasks.size})"
    }

    private fun updateDashboardExpenses(expenses: List<Expense>) {
        val total = expenses.sumOf { it.amount }
        binding.tvTotalExpense.text = String.format(Locale.getDefault(), "%.2f€", total)

        if (expenses.isEmpty()) {
            binding.viewPieChart.setData(emptyList())
            // Opcional: ocultar o mostrar mensaje de "Sin gastos"
            return
        }

        // Agrupar por categoría
        val byCategory = expenses.groupBy { it.category }
        val chartData = mutableListOf<Pair<Float, Int>>()
        
        // Colores predefinidos para las categorías
        val colors = listOf(
            0xFF1565C0.toInt(), // Azul Oscuro
            0xFF1E88E5.toInt(), // Azul Medio
            0xFF42A5F5.toInt(), // Azul Claro
            0xFF90CAF9.toInt(), // Azul Muy Claro
            0xFFBBDEFB.toInt(), // Azul Pálido
            0xFFE3F2FD.toInt()  // Azul Blanquecino
        )

        var colorIndex = 0
        byCategory.forEach { (category, list) ->
            val catTotal = list.sumOf { it.amount }
            val percentage = (catTotal / total * 100).toFloat()
            chartData.add(Pair(percentage, colors[colorIndex % colors.size]))
            colorIndex++
        }

        binding.viewPieChart.setData(chartData)
        
        // Actualizar barras (simple visual hack para el ejemplo, o podrías implementar un BarChart real)
        updateBarChart(byCategory, total)
    }

    private fun updateBarChart(byCategory: Map<String, List<Expense>>, total: Double) {
        val barContainer = binding.viewBarChart
        barContainer.removeAllViews()
        
        val colors = listOf(0xFF1565C0.toInt(), 0xFF1E88E5.toInt(), 0xFF42A5F5.toInt(), 0xFF90CAF9.toInt())
        var colorIndex = 0
        
        val sortedCategories = byCategory.entries.sortedByDescending { it.value.sumOf { e -> e.amount } }
        
        for (entry in sortedCategories) {
            val catTotal = entry.value.sumOf { it.amount }
            val weight = (catTotal / total).toFloat()
            
            val bar = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, (200 * weight * resources.displayMetrics.density).toInt(), 1f).apply {
                    setMargins(8, 0, 8, 0)
                }
                setBackgroundColor(colors[colorIndex % colors.size])
            }
            barContainer.addView(bar)
            colorIndex++
        }
    }

    private fun updateDashboardRoles(roles: List<Role>, members: List<EventRoleMember>) {
        val totalRoles = roles.size
        val rolesCovered = roles.count { role ->
            val minNeeded = role.minPeople ?: if (role.isMandatory) 1 else 0
            val currentCount = members.count { it.roleId == role.id }
            currentCount >= minNeeded
        }
        
        binding.tvRolesCovered.text = "$rolesCovered de $totalRoles"
    }

    private fun setupTabs() {
        binding.tabLayout.removeAllTabs()
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.tab_roles)))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.tab_expenses)))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.tab_dashboard)))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Info"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateVisibility(tab?.position ?: 0)
                updateAdminUi()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Seleccionar tab de Información por defecto (index 3)
        binding.tabLayout.getTabAt(3)?.select()
        updateVisibility(3)
    }

    private fun updateVisibility(position: Int) {
        binding.rvRoles.visibility = if (position == 0) View.VISIBLE else View.GONE
        binding.rvExpenses.visibility = if (position == 1) View.VISIBLE else View.GONE
        binding.layoutDashboard.visibility = if (position == 2) View.VISIBLE else View.GONE
        binding.layoutInfo.visibility = if (position == 3) View.VISIBLE else View.GONE
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

        tvName.text = expense.name
        tvAmount.text = String.format(Locale.getDefault(), "%.2f€", expense.amount)
        tvType.text = "Categoría: ${expense.category}"

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
        binding.rvParticipantsDetail.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = participantAdapter
        }
    }

    private fun loadData() {
        currentEventId?.let { id ->
            viewModel.loadEvent(id)
            viewModel.loadParticipants(id)
            viewModel.loadRoles(id)
            viewModel.loadRoleMembers(id)
            viewModel.loadExpenses(id)
            viewModel.loadTasks(id)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
