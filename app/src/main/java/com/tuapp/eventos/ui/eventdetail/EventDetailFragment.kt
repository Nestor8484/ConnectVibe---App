package com.tuapp.eventos.ui.eventdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
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
import kotlinx.coroutines.flow.combine
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

    private val participantAdapter = ParticipantAdapter(
        onAdminPromotion = { userId, isAdmin ->
            currentEventId?.let { eventId ->
                viewModel.updateParticipantRole(eventId, userId, isAdmin)
            }
        },
        onRemoveMember = { userId ->
            currentEventId?.let { eventId ->
                viewModel.removeParticipant(eventId, userId)
            }
        }
    )

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
            }
        }
    }

    private fun canUserLeave(): Boolean {
        val participants = viewModel.participants.value
        val adminCount = participants.count { it.role == MemberRole.ADMIN }
        return !isUserAdmin || adminCount > 1
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
            val status = currentEvent?.status ?: "pending"
            val selectedTab = binding.tabLayout.selectedTabPosition
            
            // El botón de acción (Iniciar/Finalizar) solo se muestra en la pestaña de Información
            binding.btnStatusAction.visibility = if (selectedTab == 3) View.VISIBLE else View.GONE
            when (status) {
                "pending" -> {
                    binding.btnStatusAction.text = "Iniciar"
                }
                "started" -> {
                    binding.btnStatusAction.text = "Finalizar"
                }
                "finished" -> {
                    binding.btnStatusAction.visibility = View.GONE
                }
            }
        } else {
            binding.btnStatusAction.visibility = View.GONE
        }

        roleAdapter.setAdminStatus(isUserAdmin && status == "pending")
        
        val canLeave = canUserLeave()
        binding.cbParticipateDetail.isEnabled = canLeave
        binding.cbParticipateDetail.alpha = if (canLeave) 1.0f else 0.6f
        
        // El checkbox siempre debe ser visible para permitir unirse si no se participa, 
        // o para ver que se participa. El requerimiento del usuario "desaparecer el check" 
        // se refiere a la LISTA (EventAdapter), donde si no participas no debe salir el indicador "Asistiendo".
        // En el detalle, el checkbox es el mecanismo para unirse/abandonar.
        binding.cbParticipateDetail.visibility = View.VISIBLE
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
                        binding.cbParticipateDetail.isEnabled = canUserLeave()
                        Toast.makeText(context, "Estado de participación actualizado", Toast.LENGTH_SHORT).show()
                        viewModel.resetJoinState()
                    }
                    is EventViewModel.JoinEventState.Error -> {
                        binding.cbParticipateDetail.isEnabled = canUserLeave()
                        binding.cbParticipateDetail.isChecked = !binding.cbParticipateDetail.isChecked
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetJoinState()
                    }
                    else -> {
                        binding.cbParticipateDetail.isEnabled = canUserLeave()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.expenseOpState.collectLatest { state ->
                when (state) {
                    is EventViewModel.RoleOpState.Success -> {
                        Toast.makeText(context, "Operación de gasto realizada", Toast.LENGTH_SHORT).show()
                        viewModel.resetExpenseOpState()
                    }
                    is EventViewModel.RoleOpState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetExpenseOpState()
                    }
                    else -> {}
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.roleOpState.collectLatest { state ->
                when (state) {
                    is EventViewModel.RoleOpState.Success -> {
                        Toast.makeText(context, "Rol actualizado correctamente", Toast.LENGTH_SHORT).show()
                        viewModel.resetRoleOpState()
                    }
                    is EventViewModel.RoleOpState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetRoleOpState()
                    }
                    else -> {}
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isParticipating.collectLatest { isParticipating ->
                binding.cbParticipateDetail.isChecked = isParticipating
                // Si el usuario no participa, mostramos el texto para unirse
                binding.cbParticipateDetail.text = if (isParticipating) "Participando en este evento" else "Participar en este evento"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.participants, viewModel.event) { participants, event ->
                participants to event
            }.collectLatest { (participants, event) ->
                binding.tvParticipantCount.text = "${participants.size} participantes"
                participantAdapter.submitList(participants)
                
                // Re-check admin status when participants list is updated
                val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val member = participants.find { it.userId == userId }
                    isUserAdmin = member?.role == MemberRole.ADMIN
                    updateAdminUi()
                }
                
                participantAdapter.setEventDetails(event?.createdBy, isUserAdmin)
                roleAdapter.setAdminStatus(isUserAdmin && event?.status == "pending")
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.collectLatest { event ->
                event?.let {
                    roleAdapter.setEventStatus(it.status)
                    
                    // Actualizar UI con datos reales del evento
                    binding.tvEventTitleContainer.text = it.name
                    binding.tvInfoDescription.text = it.description ?: "Sin descripción"
                    
                    val tvDate = binding.layoutInfo.findViewById<TextView>(R.id.tvInfoDate)
                    val dateFormat = java.text.SimpleDateFormat("dd 'de' MMMM, yyyy", java.util.Locale.getDefault())
                    tvDate?.text = it.startDate?.let { date -> dateFormat.format(date) } ?: getString(R.string.no_date)

                    val tvLocation = binding.layoutInfo.findViewById<TextView>(R.id.tvInfoLocation)
                    tvLocation?.text = it.location ?: "Ubicación no especificada"
                    
                    // Aplicar color del evento dinámicamente SOLO en esta pantalla
                    it.color?.let { colorStr ->
                        try {
                            val colorInt = colorStr.toColorInt()
                            
                            // Actualizar color en el adaptador de gastos
                            expenseAdapter.updateEventColor(colorStr)
                            
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
                                
                                // Y para Fecha
                                val dateView = binding.layoutInfo.findViewById<TextView>(R.id.tvInfoDate)
                                val dateIndex = parent?.indexOfChild(dateView) ?: -1
                                if (dateIndex > 0) (parent?.getChildAt(dateIndex - 1) as? TextView)?.setTextColor(colorInt)

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

                            // 6. Dashboard - Card de Resumen y Progreso
                            val summaryCard = binding.layoutDashboard.getChildAt(0) as? com.google.android.material.card.MaterialCardView
                            summaryCard?.setCardBackgroundColor(alphaColor)
                            summaryCard?.strokeColor = colorInt
                            summaryCard?.strokeWidth = (2 * density).toInt()
                            
                            binding.tvTotalExpense.setTextColor(colorInt)
                            binding.tvRolesCovered.setTextColor(colorInt)
                            
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
            viewModel.expenseOpState.collectLatest { state ->
                when (state) {
                    is EventViewModel.RoleOpState.Success -> {
                        Toast.makeText(context, "Operación de gasto realizada", Toast.LENGTH_SHORT).show()
                        viewModel.resetExpenseOpState()
                    }
                    is EventViewModel.RoleOpState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetExpenseOpState()
                    }
                    else -> {}
                }
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

        // Observar estado de operaciones de gastos
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.expenseOpState.collectLatest { state ->
                when (state) {
                    is EventViewModel.RoleOpState.Loading -> {
                        // Podrías mostrar un loading en el botón del diálogo si fuera necesario
                    }
                    is EventViewModel.RoleOpState.Success -> {
                        Toast.makeText(context, "Gasto guardado correctamente", Toast.LENGTH_SHORT).show()
                        viewModel.resetExpenseOpState()
                    }
                    is EventViewModel.RoleOpState.Error -> {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Error")
                            .setMessage("No se pudo guardar el gasto: ${state.message}")
                            .setPositiveButton("Aceptar", null)
                            .show()
                        viewModel.resetExpenseOpState()
                    }
                    else -> {}
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
    }

    private fun updateDashboardExpenses(expenses: List<Expense>) {
        val total = expenses.sumOf { it.amount }
        binding.tvTotalExpense.text = String.format(Locale.getDefault(), "%.2f€", total)

        val eventColorStr = viewModel.event.value?.color
        val eventColorInt = try {
            eventColorStr?.toColorInt() ?: 0xFF1565C0.toInt()
        } catch (e: Exception) {
            0xFF1565C0.toInt()
        }

        if (expenses.isEmpty()) {
            binding.viewPieChart.setData(emptyList())
            // Limpiar leyenda si no hay gastos
            binding.layoutExpenseLegend.removeAllViews()
            return
        }

        // Agrupar por categoría
        val byCategory = expenses.groupBy { it.category }
        val chartData = mutableListOf<Pair<Float, Int>>()
        
        // Generar variantes del color del evento para las categorías
        var colorIndex = 0
        val sortedEntries = byCategory.entries.sortedByDescending { it.value.sumOf { exp -> exp.amount } }
        
        // Obtener el GridLayout de la leyenda
        val gridLayout = binding.layoutExpenseLegend
        gridLayout.removeAllViews()

        sortedEntries.forEach { (category, list) ->
            val catTotal = list.sumOf { it.amount }
            val percentage = (catTotal / total * 100).toFloat()
            
            // Variamos la opacidad o el brillo para distinguir categorías usando el color base del evento
            val alpha = (1.0 - (colorIndex * 0.15)).coerceAtLeast(0.3)
            val categoryColor = ( (alpha * 255).toInt() shl 24 ) or (eventColorInt and 0x00FFFFFF)
            
            chartData.add(Pair(percentage, categoryColor))
            
            // Añadir a la leyenda dinámica
            val legendItem = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(8, 6, 8, 6)
                
                val params = android.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                    setMargins(0, 0, 0, 0)
                }
                layoutParams = params
                
                val colorView = View(context).apply {
                    val size = (12 * resources.displayMetrics.density).toInt()
                    layoutParams = LinearLayout.LayoutParams(size, size)
                    setBackgroundColor(categoryColor)
                }
                
                val textView = TextView(context).apply {
                    text = " $category (${String.format("%.2f€", catTotal)})"
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        marginStart = (8 * resources.displayMetrics.density).toInt()
                    }
                }
                
                addView(colorView)
                addView(textView)
            }
            gridLayout?.addView(legendItem)

            colorIndex++
        }

        binding.viewPieChart.setData(chartData)
        
        updateBarChart(byCategory, total, eventColorInt)
    }

    private fun updateBarChart(byCategory: Map<String, List<Expense>>, total: Double, eventColorInt: Int) {
        val barContainer = binding.viewBarChart
        barContainer.removeAllViews()
        
        var colorIndex = 0
        val sortedCategories = byCategory.entries.sortedByDescending { entry -> 
            entry.value.sumOf { exp: Expense -> exp.amount } 
        }
        
        for (entry in sortedCategories) {
            val catTotal = entry.value.sumOf { it.amount }
            val weight = (catTotal / total).toFloat()
            
            val alpha = (1.0 - (colorIndex * 0.15)).coerceAtLeast(0.3)
            val categoryColor = ( (alpha * 255).toInt() shl 24 ) or (eventColorInt and 0x00FFFFFF)
            
            val bar = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, (200 * weight * resources.displayMetrics.density).toInt(), 1f).apply {
                    setMargins(8, 0, 8, 0)
                }
                setBackgroundColor(categoryColor)
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

        // Aplicar color del evento
        val colorStr = viewModel.event.value?.color
        colorStr?.let { cStr ->
            try {
                val colorInt = android.graphics.Color.parseColor(cStr)
                val card = dialogView as? com.google.android.material.card.MaterialCardView
                card?.strokeColor = colorInt
                card?.strokeWidth = (2 * resources.displayMetrics.density).toInt()
                
                dialogView.findViewById<TextView>(R.id.tvDialogRoleName)?.setTextColor(colorInt)
                dialogView.findViewById<MaterialButton>(R.id.btnAssign)?.backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)
                dialogView.findViewById<TextView>(R.id.tvRoleStatus)?.let { 
                    (it.background as? android.graphics.drawable.GradientDrawable)?.setStroke((1 * resources.displayMetrics.density).toInt(), colorInt)
                    it.setTextColor(colorInt)
                }
            } catch (e: Exception) {}
        }

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
        
        val currentUserId = SupabaseModule.client.auth.currentUserOrNull()?.id
        val status = viewModel.event.value?.status ?: "pending"
        val isEventFinished = status == "finished"
        
        // Mostrar miembros asignados
        val membersInRole = viewModel.roleMembers.value.filter { it.roleId == role.id }
        if (membersInRole.isNotEmpty()) {
            tvLabelMembers.visibility = View.VISIBLE
            tvAssignedMembers.visibility = View.VISIBLE
            
            val memberNames = membersInRole.map { member ->
                viewModel.participants.value.find { it.userId == member.userId }?.userName ?: "Usuario"
            }.joinToString(", ")
            
            tvAssignedMembers.text = memberNames
            
            // Si es admin, permitir desasignar a otros clicando en el texto o con un menú
            if (isUserAdmin && status == "pending") {
                tvAssignedMembers.setOnClickListener {
                    val membersData = membersInRole.map { member ->
                        val p = viewModel.participants.value.find { it.userId == member.userId }
                        val name = p?.userName ?: "Usuario"
                        val username = p?.email?.removePrefix("@") ?: ""
                        name to username
                    }
                    
                    val items = membersData.map { (name, username) ->
                        if (username.isNotEmpty()) "$name (@$username)" else name
                    }.toTypedArray()
                    
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.unassign_from_role)
                        .setItems(items) { _, which ->
                            val memberToUnassign = membersInRole[which]
                            viewModel.toggleRoleAssignment(role.id!!, memberToUnassign.userId, currentEventId!!, false)
                            dialog.dismiss()
                        }
                        .show()
                }
                tvAssignedMembers.append(" (${getString(R.string.unassign_from_role)})")
            }
        } else {
            tvLabelMembers.visibility = View.VISIBLE
            tvAssignedMembers.text = "Nadie asignado aún"
            tvAssignedMembers.setOnClickListener(null)
        }

        val isUserAssigned = membersInRole.any { it.userId == currentUserId }

        btnAssign.text = if (isUserAssigned) "Desasignarme" else "Asignarme"
        btnAssign.setIconResource(if (isUserAssigned) android.R.drawable.ic_menu_close_clear_cancel else android.R.drawable.ic_input_add)

        // Deshabilitar asignación si el evento ha finalizado
        if (isEventFinished) {
            btnAssign.isEnabled = false
            btnAssign.alpha = 0.5f
            btnAssign.text = if (isUserAssigned) "Asignado (Evento finalizado)" else "No asignado (Evento finalizado)"
        } else {
            btnAssign.isEnabled = true
            btnAssign.alpha = 1.0f
            btnAssign.setOnClickListener { 
                if (currentUserId != null && currentEventId != null) {
                    viewModel.toggleRoleAssignment(role.id!!, currentUserId, currentEventId!!, !isUserAssigned)
                    dialog.dismiss()
                }
            }
        }
        
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

        // Aplicar color del evento
        val colorStr = viewModel.event.value?.color
        colorStr?.let { cStr ->
            try {
                val colorInt = android.graphics.Color.parseColor(cStr)
                val card = dialogView as? com.google.android.material.card.MaterialCardView
                card?.strokeColor = colorInt
                card?.strokeWidth = (2 * resources.displayMetrics.density).toInt()
                
                dialogView.findViewById<TextView>(R.id.tvDialogExpenseName)?.setTextColor(colorInt)
                dialogView.findViewById<TextView>(R.id.tvDialogExpenseAmount)?.setTextColor(colorInt)
            } catch (e: Exception) {}
        }

        val tvName = dialogView.findViewById<TextView>(R.id.tvDialogExpenseName)
        val tvAmount = dialogView.findViewById<TextView>(R.id.tvDialogExpenseAmount)
        val tvType = dialogView.findViewById<TextView>(R.id.tvDialogExpenseType)
        val btnClose = dialogView.findViewById<MaterialButton>(R.id.btnCloseExpense)
        val btnEdit = dialogView.findViewById<MaterialButton>(R.id.btnEditExpense)
        val btnDelete = dialogView.findViewById<MaterialButton>(R.id.btnDeleteExpense)

        tvName.text = expense.title
        tvAmount.text = String.format(Locale.getDefault(), "%.2f€", expense.amount)
        tvType.text = "Categoría: ${expense.category}"
        val tvDesc = dialogView.findViewById<TextView>(R.id.tvDialogExpenseDescription)
        tvDesc?.text = expense.description ?: "Sin descripción"
        tvDesc?.visibility = if (expense.description.isNullOrBlank()) View.GONE else View.VISIBLE

        // Detalles del pago
        val tvDetails = dialogView.findViewById<TextView>(R.id.tvDialogExpenseDetails)
        val payer = viewModel.participants.value.find { it.userId == expense.paidByUserId }?.userName ?: "Desconocido"
        val date = expense.incurredAt?.let { java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "No especificada"
        
        tvDetails?.text = "• Pagado por: $payer\n• Fecha: $date"

        // Lógica de autorización para editar
        val currentUserId = SupabaseModule.client.auth.currentUserOrNull()?.id
        val canEdit = isUserAdmin || expense.createdBy == currentUserId
        
        btnEdit.visibility = if (canEdit) View.VISIBLE else View.GONE
        btnEdit.setOnClickListener {
            dialog.dismiss()
            showEditExpenseDialog(expense)
        }

        btnDelete.visibility = if (canEdit) View.VISIBLE else View.GONE
        btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar Gasto")
                .setMessage("¿Estás seguro de que quieres eliminar este gasto?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar") { _, _ ->
                    currentEventId?.let { eventId ->
                        expense.id?.let { expenseId ->
                            viewModel.deleteExpense(eventId, expenseId)
                            dialog.dismiss()
                        }
                    }
                }
                .show()
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()

        // Forzar ancho al 90%
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showEditExpenseDialog(expense: Expense) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_expense, null)
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_ConnectVibe_Dialog)
            .setView(dialogView)
            .create()

        // Aplicar color del evento
        val colorStr = viewModel.event.value?.color
        colorStr?.let { cStr ->
            try {
                val colorInt = android.graphics.Color.parseColor(cStr)
                val card = dialogView as? com.google.android.material.card.MaterialCardView
                card?.strokeColor = colorInt
                card?.strokeWidth = (2 * resources.displayMetrics.density).toInt()
                
                dialogView.findViewById<TextView>(R.id.tvDialogExpenseTitle)?.setTextColor(colorInt)
                dialogView.findViewById<MaterialButton>(R.id.btnCreateExpense)?.backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)
                
                val container = dialogView.findViewById<TextView>(R.id.tvDialogExpenseTitle).parent as? ViewGroup
                container?.let {
                    for (i in 0 until it.childCount) {
                        val child = it.getChildAt(i)
                        if (child is com.google.android.material.textfield.TextInputLayout) {
                            child.boxStrokeColor = colorInt
                            child.defaultHintTextColor = android.content.res.ColorStateList.valueOf(colorInt)
                        }
                    }
                }
            } catch (e: Exception) {}
        }

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogExpenseTitle)
        tvTitle?.text = "Editar Gasto"
        
        val etName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etExpenseName)
        val etCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.etExpenseCategory)
        val etAmount = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etExpenseAmount)
        val etDescription = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etExpenseDescription)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnCreateExpense)
        
        // Ocultar campo estrategia ya que no se usa según lo pedido
        dialogView.findViewById<View>(R.id.tilExpenseStrategy)?.visibility = View.GONE

        // Rellenar datos actuales
        etName?.setText(expense.title)
        etAmount?.setText(expense.amount.toString())
        etDescription?.setText(expense.description)
        etCategory?.setText(expense.category, false)
        
        val categories = arrayOf("Comida", "Bebida", "Transporte", "Alojamiento", "Alquiler", "Decoración", "Entretenimiento", "Otros")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        etCategory?.setAdapter(adapter)

        btnSave?.text = "Actualizar Gasto"
        btnSave?.setOnClickListener {
            val name = etName?.text.toString()
            val amount = etAmount?.text.toString().toDoubleOrNull() ?: 0.0
            val category = etCategory?.text.toString()
            val description = etDescription?.text.toString()

            if (name.isNotBlank() && amount > 0 && category.isNotBlank()) {
                val updatedExpense = expense.copy(
                    title = name,
                    amount = amount,
                    category = category,
                    description = description
                )
                
                // Usamos la lógica de actualización
                currentEventId?.let { eventId ->
                    viewModel.updateExpense(eventId, updatedExpense)
                }
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Por favor, rellena todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showCreateExpenseDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_expense, null)
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_ConnectVibe_Dialog)
            .setView(dialogView)
            .create()

        // Aplicar color del evento
        val colorStr = viewModel.event.value?.color
        colorStr?.let { cStr ->
            try {
                val colorInt = android.graphics.Color.parseColor(cStr)
                val card = dialogView as? com.google.android.material.card.MaterialCardView
                card?.strokeColor = colorInt
                card?.strokeWidth = (2 * resources.displayMetrics.density).toInt()
                
                dialogView.findViewById<TextView>(R.id.tvDialogExpenseTitle)?.setTextColor(colorInt)
                dialogView.findViewById<MaterialButton>(R.id.btnCreateExpense)?.backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)
                
                // Aplicar a los TextInputLayouts
                val container = dialogView.findViewById<LinearLayout>(R.id.tvDialogExpenseTitle).parent as? ViewGroup
                container?.let {
                    for (i in 0 until it.childCount) {
                        val child = it.getChildAt(i)
                        if (child is com.google.android.material.textfield.TextInputLayout) {
                            child.boxStrokeColor = colorInt
                            child.defaultHintTextColor = android.content.res.ColorStateList.valueOf(colorInt)
                        }
                    }
                }
            } catch (e: Exception) {}
        }

        val categories = arrayOf("Comida", "Bebida", "Transporte", "Alojamiento", "Alquiler", "Decoración", "Entretenimiento", "Otros")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        val autoCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.etExpenseCategory)
        autoCategory.setAdapter(adapter)

        // Ocultar campo estrategia en creación también
        dialogView.findViewById<View>(R.id.tilExpenseStrategy)?.visibility = View.GONE

        val etName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etExpenseName)
        val etDescription = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etExpenseDescription)
        val etAmount = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etExpenseAmount)
        val btnCreate = dialogView.findViewById<MaterialButton>(R.id.btnCreateExpense)

        btnCreate.setOnClickListener {
            val name = etName.text.toString()
            val category = autoCategory.text.toString()
            val description = etDescription?.text.toString()
            val amountStr = etAmount.text.toString()
            val eventId = currentEventId

            if (name.isBlank() || category.isBlank() || amountStr.isBlank() || eventId == null) {
                Toast.makeText(context, "Por favor, rellena todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(context, "Introduce un importe válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
            val expense = Expense(
                eventId = eventId,
                createdBy = userId ?: "",
                title = name,
                amount = amount,
                category = category,
                description = description,
                paidByUserId = userId,
                incurredAt = Date()
            )

            viewModel.addExpense(eventId, expense)
            dialog.dismiss()
        }

        dialog.show()

        // Forzar ancho al 90%
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
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
            // viewModel.loadTasks(id) // Deshabilitado: tabla event_tasks no existe
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
