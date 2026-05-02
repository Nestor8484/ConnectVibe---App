package com.tuapp.eventos.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.eventos.R
import com.tuapp.eventos.data.model.GroupMember
import com.tuapp.eventos.data.model.Profile
import com.tuapp.eventos.data.repository.GroupRepository
import com.tuapp.eventos.databinding.FragmentGroupDetailBinding
import com.tuapp.eventos.di.SupabaseModule
import com.tuapp.eventos.domain.model.Event
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Date

class GroupDetailFragment : Fragment() {

    private var _binding: FragmentGroupDetailBinding? = null
    private val binding get() = _binding!!

    private val eventViewModel: EventViewModel by viewModels()
    private val memberViewModel: MemberViewModel by viewModels()
    private val groupViewModel: GroupViewModel by viewModels()
    private var isUserAdmin = false
    private var allEvents: List<Event> = emptyList()
    private var allGroupExpenses: List<com.tuapp.eventos.domain.model.Expense> = emptyList()
    private val eventStatusAdapter = EventStatusAdapter()

    private val eventAdapter = EventAdapter(
        onEventClick = { event ->
            val bundle = Bundle().apply {
                putString("eventId", event.id)
                putString("eventTitle", event.name)
                putString("eventDescription", event.description)
            }
            findNavController().navigate(R.id.action_global_eventDetailFragment, bundle)
        }
    )

    private val memberAdapter: MemberAdapter by lazy {
        MemberAdapter(
            onPromoteAdmin = { userId, isAdmin ->
                arguments?.getString("groupId")?.let { groupId ->
                    memberViewModel.updateMemberRole(groupId, userId, isAdmin)
                }
            },
            onRemoveMember = { userId ->
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Eliminar Miembro")
                    .setMessage("¿Estás seguro de que quieres eliminar a este miembro del grupo?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        arguments?.getString("groupId")?.let { groupId ->
                            memberViewModel.removeMember(groupId, userId)
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val groupId = arguments?.getString("groupId") ?: ""
        val groupName = arguments?.getString("groupName") ?: "Grupo"
        binding.tvGroupName.text = groupName

        setupRecyclerViews()
        setupTabs()
        setupDashboardToggle()
        observeViewModel()
        setupFilters()
        setupDashboard()
        loadGroupData(groupId)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.ivUserProfile.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        binding.fabAddGroupEvent.setOnClickListener {
            val bundle = Bundle().apply {
                putString("groupId", groupId)
            }
            findNavController().navigate(R.id.createEventFragment, bundle)
        }

        binding.includeMembers.fabAddMember.setOnClickListener {
            val bundle = Bundle().apply {
                putString("groupId", groupId)
            }
            findNavController().navigate(R.id.addMemberFragment, bundle)
        }

        binding.includeInfo.btnEditGroup.setOnClickListener {
            val bundle = Bundle().apply {
                putString("groupId", groupId)
            }
            findNavController().navigate(R.id.action_groupDetailFragment_to_editGroupFragment, bundle)
        }
    }

    private fun setupFilters() {
        val options = arrayOf("Todos", "Pendientes", "En curso", "Finalizados")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        binding.actvEventFilter.setAdapter(adapter)
        
        // Mantener la selección actual o poner Pendientes por defecto
        val currentText = binding.actvEventFilter.text.toString()
        if (currentText.isEmpty() || !options.contains(currentText)) {
            binding.actvEventFilter.setText(options[1], false)
        }

        binding.actvEventFilter.setOnItemClickListener { _, _, position, _ ->
            filterEvents(options[position])
        }
    }

    private fun filterEvents(filter: String) {
        val filtered = when (filter) {
            "Pendientes" -> allEvents.filter { it.status == "pending" }
            "En curso" -> allEvents.filter { it.status == "started" }
            "Finalizados" -> allEvents.filter { it.status == "finished" }
            else -> allEvents
        }
        eventAdapter.submitList(filtered)
    }

    private fun setupDashboard() {
        // Selector de meses
        val months = java.text.DateFormatSymbols().months.filter { it.isNotEmpty() }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, months)
        binding.includeDashboard.actvMonthFilter.setAdapter(adapter)
        
        // Mes actual por defecto
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
        binding.includeDashboard.actvMonthFilter.setText(months[currentMonth], false)

        binding.includeDashboard.actvMonthFilter.setOnItemClickListener { _, _, position, _ ->
            updateDashboardData()
        }

        binding.includeDashboard.swGroupByEvent.setOnCheckedChangeListener { _, _ ->
            updateDashboardData()
        }

        binding.includeDashboard.btnToggleGroupChartType.setOnClickListener {
            val isPieVisible = binding.includeDashboard.viewGroupPieChart.visibility == View.VISIBLE
            if (isPieVisible) {
                binding.includeDashboard.viewGroupPieChart.visibility = View.GONE
                binding.includeDashboard.viewGroupBarChart.visibility = View.VISIBLE
                binding.includeDashboard.btnToggleGroupChartType.text = getString(R.string.view_pie_chart)
            } else {
                binding.includeDashboard.viewGroupPieChart.visibility = View.VISIBLE
                binding.includeDashboard.viewGroupBarChart.visibility = View.GONE
                binding.includeDashboard.btnToggleGroupChartType.text = getString(R.string.view_bar_chart)
            }
        }
    }

    private fun updateDashboardData() {
        val selectedMonthName = binding.includeDashboard.actvMonthFilter.text.toString()
        val months = java.text.DateFormatSymbols().months.toList()
        val selectedMonthIndex = months.indexOf(selectedMonthName)
        
        if (selectedMonthIndex == -1) return

        // Filtrar eventos por el mes de inicio
        val calendar = java.util.Calendar.getInstance()
        val eventsInMonth = allEvents.filter { event ->
            event.startDate?.let { date ->
                calendar.time = date
                calendar.get(java.util.Calendar.MONTH) == selectedMonthIndex
            } ?: false
        }

        // Actualizar contador y lista de estados
        binding.includeDashboard.tvGroupEventCount.text = eventsInMonth.size.toString()
        eventStatusAdapter.submitList(eventsInMonth)

        // Filtrar gastos por el mes (usando la fecha del gasto si existe, o la del evento)
        val expensesInMonth = allGroupExpenses.filter { expense ->
            expense.incurredAt?.let { date ->
                calendar.time = date
                calendar.get(java.util.Calendar.MONTH) == selectedMonthIndex
            } ?: false
        }

        val totalSpent = expensesInMonth.sumOf { it.amount }
        binding.includeDashboard.tvGroupTotalExpense.text = String.format("%.2f€", totalSpent)

        val groupColor = try {
            android.graphics.Color.parseColor(groupViewModel.currentGroup.value?.color ?: "#1565C0")
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#1565C0")
        }

        // Limpiar leyenda antigua
        binding.includeDashboard.root.findViewById<android.widget.GridLayout>(R.id.layoutGroupExpenseLegend)?.removeAllViews()

        if (expensesInMonth.isEmpty()) {
            binding.includeDashboard.viewGroupPieChart.setData(emptyList())
            return
        }

        // Agrupar para la gráfica
        val groupByEvent = binding.includeDashboard.swGroupByEvent.isChecked
        val byCategoryMap = if (groupByEvent) {
            binding.includeDashboard.tvGroupChartTitle.text = "Gastos por Evento"
            expensesInMonth.groupBy { expense ->
                allEvents.find { it.id == expense.eventId }?.name ?: "Otros"
            }
        } else {
            binding.includeDashboard.tvGroupChartTitle.text = "Gastos por Categoría"
            expensesInMonth.groupBy { it.category }
        }

        val chartData = mutableListOf<Pair<Float, Int>>()
        var colorIndex = 0
        val sortedEntries = byCategoryMap.entries.sortedByDescending { it.value.sumOf { exp -> exp.amount } }
        val gridLayout = binding.includeDashboard.root.findViewById<android.widget.GridLayout>(R.id.layoutGroupExpenseLegend)

        sortedEntries.forEach { (label, list) ->
            val catTotal = list.sumOf { it.amount }
            val percentage = (catTotal / totalSpent * 100).toFloat()
            
            val alpha = (1.0 - (colorIndex * 0.15)).coerceAtLeast(0.3)
            val itemColor = ((alpha * 255).toInt() shl 24) or (groupColor and 0x00FFFFFF)
            
            chartData.add(Pair(percentage, itemColor))
            
            // Leyenda
            val legendItem = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(4, 4, 4, 4)
                val params = android.widget.GridLayout.LayoutParams()
                params.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                layoutParams = params
                
                val colorView = View(context).apply {
                    val size = (12 * resources.displayMetrics.density).toInt()
                    layoutParams = android.widget.LinearLayout.LayoutParams(size, size)
                    setBackgroundColor(itemColor)
                }
                
                val textView = android.widget.TextView(context).apply {
                    text = " $label (${String.format("%.2f€", catTotal)})"
                    textSize = 12f
                    layoutParams = android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        marginStart = (8 * resources.displayMetrics.density).toInt()
                    }
                }
                
                addView(colorView)
                addView(textView)
            }
            gridLayout?.addView(legendItem)
            colorIndex++
        }

        binding.includeDashboard.viewGroupPieChart.setData(chartData)
        updateGroupBarChart(byCategoryMap, totalSpent, groupColor)
    }

    private fun updateGroupBarChart(data: Map<String, List<com.tuapp.eventos.domain.model.Expense>>, total: Double, groupColor: Int) {
        val barContainer = binding.includeDashboard.viewGroupBarChart
        barContainer.removeAllViews()
        
        if (data.isEmpty() || total <= 0) return
        
        val maxAmount = data.values.maxOf { it.sumOf { exp -> exp.amount } }
        val density = resources.displayMetrics.density
        val maxHeight = (180 * density).toInt()

        data.entries.sortedByDescending { it.value.sumOf { exp -> exp.amount } }.take(4).forEachIndexed { index, entry ->
            val amount = entry.value.sumOf { it.amount }
            val height = ((amount / maxAmount) * maxHeight).toInt().coerceAtLeast((10 * density).toInt())
            
            val alpha = (1.0 - (index * 0.15)).coerceAtLeast(0.3)
            val barColor = ((alpha * 255).toInt() shl 24) or (groupColor and 0x00FFFFFF)

            val bar = View(context).apply {
                val params = android.widget.LinearLayout.LayoutParams(0, height, 1f).apply {
                    leftMargin = (8 * density).toInt()
                    rightMargin = (8 * density).toInt()
                }
                layoutParams = params
                setBackgroundColor(barColor)
                contentDescription = "${entry.key}: ${String.format("%.2f€", amount)}"
            }
            barContainer.addView(bar)
        }
    }

    private fun setupDashboardToggle() {
        binding.includeDashboard.btnToggleGroupChartType.setOnClickListener {
            val isPieVisible = binding.includeDashboard.viewGroupPieChart.visibility == View.VISIBLE
            if (isPieVisible) {
                binding.includeDashboard.viewGroupPieChart.visibility = View.GONE
                binding.includeDashboard.viewGroupBarChart.visibility = View.VISIBLE
                binding.includeDashboard.btnToggleGroupChartType.text = getString(R.string.view_pie_chart)
            } else {
                binding.includeDashboard.viewGroupPieChart.visibility = View.VISIBLE
                binding.includeDashboard.viewGroupBarChart.visibility = View.GONE
                binding.includeDashboard.btnToggleGroupChartType.text = getString(R.string.view_bar_chart)
            }
        }
    }

    private fun setupRecyclerViews() {
        binding.rvGroupEvents.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = eventAdapter
        }

        binding.includeMembers.rvMembers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = memberAdapter
        }

        binding.includeDashboard.rvGroupEventStatus.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = eventStatusAdapter
        }
    }

    private fun setupTabs() {
        binding.groupTabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showEvents()
                    1 -> showDashboard()
                    2 -> showMembers()
                    3 -> showInfo()
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun showEvents() {
        binding.layoutGroupEvents.visibility = View.VISIBLE
        binding.includeDashboard.root.visibility = View.GONE
        binding.includeMembers.root.visibility = View.GONE
        binding.includeInfo.root.visibility = View.GONE
        binding.fabAddGroupEvent.visibility = View.VISIBLE
    }

    private fun showDashboard() {
        binding.layoutGroupEvents.visibility = View.GONE
        binding.includeDashboard.root.visibility = View.VISIBLE
        binding.includeMembers.root.visibility = View.GONE
        binding.includeInfo.root.visibility = View.GONE
        binding.fabAddGroupEvent.visibility = View.GONE
    }

    private fun showMembers() {
        binding.layoutGroupEvents.visibility = View.GONE
        binding.includeDashboard.root.visibility = View.GONE
        binding.includeMembers.root.visibility = View.VISIBLE
        binding.includeInfo.root.visibility = View.GONE
        binding.fabAddGroupEvent.visibility = View.GONE
    }

    private fun showInfo() {
        binding.layoutGroupEvents.visibility = View.GONE
        binding.includeDashboard.root.visibility = View.GONE
        binding.includeMembers.root.visibility = View.GONE
        binding.includeInfo.root.visibility = View.VISIBLE
        binding.fabAddGroupEvent.visibility = View.GONE
        
        // Cargar datos en la vista de info
        groupViewModel.currentGroup.value?.let { group ->
            binding.includeInfo.tvGroupDescription.text = group.description ?: "Sin descripción"
            binding.includeInfo.tvGroupIconName.text = group.icon ?: "Grupo de Amigos"
            
            val iconRes = when (group.icon) {
                "Grupo de Amigos" -> R.drawable.ic_groups
                else -> android.R.drawable.ic_menu_myplaces
            }
            binding.includeInfo.ivGroupInfoIcon.setImageResource(iconRes)
            
            group.color?.let { colorStr ->
                try {
                    val colorInt = android.graphics.Color.parseColor(colorStr)
                    binding.includeInfo.vGroupColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)
                    binding.includeInfo.tvInfoTitle.setTextColor(colorInt)
                    binding.includeInfo.ivGroupInfoIcon.imageTintList = android.content.res.ColorStateList.valueOf(colorInt)
                } catch (e: Exception) {}
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            eventViewModel.eventsState.collectLatest { state ->
                when (state) {
                    is EventViewModel.EventsState.Loading -> {
                    }
                    is EventViewModel.EventsState.Success -> {
                        allEvents = state.events
                        filterEvents(binding.actvEventFilter.text.toString())
                    }
                    is EventViewModel.EventsState.Error -> {
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                memberViewModel.membersState,
                groupViewModel.currentGroup
            ) { membersState, currentGroup ->
                membersState to currentGroup
            }.collectLatest { (state, group) ->
                when (state) {
                    is MemberViewModel.MembersState.Loading -> {
                        // Opcional: mostrar un shimmer o cargando específico para la pestaña
                    }
                    is MemberViewModel.MembersState.Success -> {
                        if (group != null) {
                            android.util.Log.d("GroupDetail", "Syncing ${state.members.size} members for group ${group.name}")
                            memberAdapter.submitList(state.members)
                            
                            val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
                            val currentMember = state.members.find { it.first.user_id == userId }
                            val isUserCreator = group.created_by == userId
                            
                            // Un usuario es admin si tiene el flag is_admin O si es el creador
                            isUserAdmin = currentMember?.first?.is_admin == true || isUserCreator
                            
                            // Actualizar visibilidad de botones de gestión
                            binding.fabAddGroupEvent.visibility = if (isUserAdmin) View.VISIBLE else View.GONE
                            binding.includeInfo.btnEditGroup.visibility = if (isUserAdmin) View.VISIBLE else View.GONE
                            
                            memberAdapter.setAdminStatus(isUserAdmin, isUserCreator, group.created_by)

                            if (state.members.isEmpty()) {
                                android.widget.Toast.makeText(context, "No hay miembros en este grupo", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    is MemberViewModel.MembersState.Error -> {
                        android.widget.Toast.makeText(context, state.message, android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            memberViewModel.memberOpState.collectLatest { state ->
                when (state) {
                    is MemberViewModel.MemberOpState.Success -> {
                        Toast.makeText(context, "Operación realizada con éxito", Toast.LENGTH_SHORT).show()
                        memberViewModel.resetMemberOpState()
                    }
                    is MemberViewModel.MemberOpState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        memberViewModel.resetMemberOpState()
                    }
                    else -> {}
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            eventViewModel.expenses.collectLatest { expenses ->
                allGroupExpenses = expenses
                updateDashboardData()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            groupViewModel.currentGroup.collectLatest { group ->
                group?.let {
                    binding.tvGroupName.text = it.name
                    it.color?.let { color -> 
                        applyGroupStyle(color)
                    }
                    
                    val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
                    if (userId != null) {
                        it.id?.let { id -> eventViewModel.loadGroupExpenses(id) }
                    }
                }
            }
        }
    }

    private fun applyGroupStyle(colorStr: String) {
        try {
            val colorInt = android.graphics.Color.parseColor(colorStr)
            val alphaColor = (0.15 * 255).toInt() shl 24 or (colorInt and 0x00FFFFFF)
            val density = resources.displayMetrics.density

            // 1. Título (Fondo suave + borde intenso)
            val titleBg = binding.tvGroupName.background?.mutate() as? android.graphics.drawable.GradientDrawable
            titleBg?.let {
                it.setColor(alphaColor)
                it.setStroke((3 * density).toInt(), colorInt)
            }
            binding.tvGroupName.setTextColor(colorInt)

            // 2. Tabs
            binding.groupTabLayout.setSelectedTabIndicatorColor(colorInt)
            binding.groupTabLayout.setTabTextColors(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant),
                colorInt
            )

            // 3. FAB
            binding.fabAddGroupEvent.backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)
            
            // 3.5 Chips de filtro
            val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked))
            val colors = intArrayOf(colorInt, android.graphics.Color.GRAY)
            val chipColors = android.content.res.ColorStateList(states, colors)
            
            binding.tilEventFilter.setBoxStrokeColorStateList(chipColors)
            binding.actvEventFilter.setTextColor(colorInt)
            binding.tilEventFilter.setEndIconTintList(android.content.res.ColorStateList.valueOf(colorInt))
            
            // Aplicar color a los bordes también cuando no está enfocado
            binding.tilEventFilter.defaultHintTextColor = android.content.res.ColorStateList.valueOf(colorInt)
            binding.tilEventFilter.boxStrokeColor = colorInt

            // 4. Back button y Profile icon
            binding.btnBack.imageTintList = android.content.res.ColorStateList.valueOf(colorInt)
            binding.ivUserProfile.imageTintList = android.content.res.ColorStateList.valueOf(colorInt)

            // 5. Aplicar a la pestaña de info
            binding.includeInfo.tvInfoTitle.setTextColor(colorInt)
            binding.includeInfo.vGroupColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)
            binding.includeInfo.ivGroupInfoIcon.imageTintList = android.content.res.ColorStateList.valueOf(colorInt)

        } catch (e: Exception) {
            android.util.Log.e("GroupDetail", "Error applying color: ${e.message}")
        }
    }

    private fun loadGroupData(groupId: String) {
        groupViewModel.loadGroup(groupId)
        eventViewModel.loadEventsByGroup(groupId)
        memberViewModel.loadMembers(groupId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
