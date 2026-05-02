package com.tuapp.eventos.ui.events

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.eventos.R
import com.tuapp.eventos.databinding.DialogAddRoleBinding
import com.tuapp.eventos.databinding.FragmentCreateEventBinding
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.tuapp.eventos.di.SupabaseModule
import com.tuapp.eventos.domain.model.Event
import com.tuapp.eventos.domain.model.Role
import com.tuapp.eventos.ui.eventdetail.RoleAdapter
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CreateEventFragment : Fragment() {

    private var _binding: FragmentCreateEventBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventViewModel by viewModels()
    private val rolesAdapter: RoleAdapter by lazy {
        RoleAdapter(
            onRoleClick = { /* no-op click */ },
            onDeleteClick = { role ->
                createdRoles.remove(role)
                rolesAdapter.submitList(createdRoles.toList())
                updateRolesVisibility()
            }
        )
    }
    private val createdRoles = mutableListOf<Role>()
    private var selectedEventColor: String = "#1565C0"
    private var editingEventId: String? = null
    private var startDateTime: Calendar = Calendar.getInstance()
    private var endDateTime: Calendar = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
    private val dateTimeFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    private var selectedGroupId: String? = null
    private var adminGroups: List<com.tuapp.eventos.data.model.Group> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val forcePublic = arguments?.getBoolean("forcePublic", false) ?: false
        val fixedGroupId = arguments?.getString("groupId")
        val userId = SupabaseModule.client.auth.currentUserOrNull()?.id

        when {
            fixedGroupId != null -> {
                // Caso: Desde detalle de grupo -> Siempre privado y fijo
                selectedGroupId = fixedGroupId
                binding.switchPublic.isChecked = false
                binding.switchPublic.isEnabled = false
                binding.tilGroupSelector.visibility = View.GONE
                binding.tvVisibilityHint.visibility = View.VISIBLE
                binding.tvVisibilityHint.text = getString(R.string.hint_event_belongs_to_group)
            }
            forcePublic -> {
                // Caso: Desde eventos públicos -> Siempre público
                binding.switchPublic.isChecked = true
                binding.switchPublic.isEnabled = false
                binding.tilGroupSelector.visibility = View.GONE
                binding.tvVisibilityHint.visibility = View.VISIBLE
                binding.tvVisibilityHint.text = getString(R.string.hint_creating_public_event)
            }
            else -> {
                // Caso: Desde Mis Eventos -> Opción de público o privado
                binding.switchPublic.isChecked = false // Privado por defecto
                binding.switchPublic.isEnabled = true
                binding.tvVisibilityHint.visibility = View.GONE
                
                // Si es privado, mostrar selector de grupo
                updateGroupSelectorVisibility()
                
                binding.switchPublic.setOnCheckedChangeListener { _, isChecked ->
                    updateGroupSelectorVisibility()
                }

                if (userId != null) {
                    viewModel.loadAdminGroups(userId)
                }
            }
        }

        setupRecyclerView()
        setupEventIconDropdown()
        setupEventColorSelection()
        setupDateTimePicker()
        setupGroupSelector()

        editingEventId = arguments?.getString("eventId")
        if (editingEventId != null) {
            setupEditMode()
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnAddRole.setOnClickListener {
            showAddRoleDialog()
        }

        binding.btnSaveEvent.setOnClickListener {
            saveEvent()
        }

        observeViewModel()
    }

    private fun updateGroupSelectorVisibility() {
        if (!binding.switchPublic.isChecked) {
            binding.tilGroupSelector.visibility = View.VISIBLE
        } else {
            binding.tilGroupSelector.visibility = View.GONE
            selectedGroupId = null
        }
    }

    private fun setupGroupSelector() {
        binding.actvGroupSelector.setOnItemClickListener { _, _, position, _ ->
            selectedGroupId = adminGroups[position].id
        }
    }

    private fun setupRecyclerView() {
        binding.rvRolesToCreate.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = rolesAdapter
        }
        updateRolesVisibility()
    }

    private fun updateRolesVisibility() {
        if (createdRoles.isEmpty()) {
            binding.tvEmptyRolesHint.visibility = View.VISIBLE
            binding.rvRolesToCreate.visibility = View.GONE
        } else {
            binding.tvEmptyRolesHint.visibility = View.GONE
            binding.rvRolesToCreate.visibility = View.VISIBLE
        }
    }

    private fun setupDateTimePicker() {
        binding.etStartDateTime.setText(dateTimeFormatter.format(startDateTime.time))
        binding.etEndDateTime.setText(dateTimeFormatter.format(endDateTime.time))

        binding.etStartDateTime.setOnClickListener {
            showDatePicker(isStartDate = true)
        }
        binding.etEndDateTime.setOnClickListener {
            showDatePicker(isStartDate = false)
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = if (isStartDate) startDateTime else endDateTime
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                showTimePicker(isStartDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun showTimePicker(isStartDate: Boolean) {
        val calendar = if (isStartDate) startDateTime else endDateTime
        val timePicker = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                val formatted = dateTimeFormatter.format(calendar.time)
                if (isStartDate) {
                    binding.etStartDateTime.setText(formatted)
                    // Auto-adjust end date if it's before start date
                    if (endDateTime.before(startDateTime)) {
                        endDateTime.time = startDateTime.time
                        endDateTime.add(Calendar.HOUR_OF_DAY, 1)
                        binding.etEndDateTime.setText(dateTimeFormatter.format(endDateTime.time))
                    }
                } else {
                    binding.etEndDateTime.setText(formatted)
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePicker.show()
    }

    private fun setupEventIconDropdown() {
        val icons = arrayOf("Evento General", "Fiesta", "Boda", "Deportes", "Trabajo", "Educación", "Viaje", "Comida", "Salud")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, icons)
        binding.acEventIcon.setAdapter(adapter)
        binding.acEventIcon.setText("Evento General", false)
    }

    private fun setupEventColorSelection() {
        val colors = listOf("#1565C0", "#2E7D32", "#C62828", "#F9A825", "#6A1B9A", "#EF6C00", "#00838F", "#37474F")
        binding.llEventColorContainer.removeAllViews()
        
        colors.forEach { colorStr ->
            val colorView = View(requireContext()).apply {
                val size = (36 * resources.displayMetrics.density).toInt()
                layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                    setMargins(8, 8, 8, 8)
                }
                tag = colorStr
                setOnClickListener {
                    selectedEventColor = colorStr
                    updateEventColorSelectionUI()
                }
            }
            binding.llEventColorContainer.addView(colorView)
        }

        binding.cvCustomColor.setOnClickListener {
            ColorPickerDialog.Builder(requireContext())
                .setTitle("Seleccionar Color")
                .setPreferenceName("CreateEventColorPicker")
                .setPositiveButton("Confirmar", ColorEnvelopeListener { envelope, _ ->
                    selectedEventColor = "#${envelope.hexCode}"
                    updateEventColorSelectionUI()
                })
                .setNegativeButton("Cancelar") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .attachAlphaSlideBar(false)
                .attachBrightnessSlideBar(true)
                .show()
        }

        updateEventColorSelectionUI()
    }

    private fun updateEventColorSelectionUI() {
        val predefinedColors = listOf("#1565C0", "#2E7D32", "#C62828", "#F9A825", "#6A1B9A", "#EF6C00", "#00838F", "#37474F")
        var anyPredefinedSelected = false

        for (i in 0 until binding.llEventColorContainer.childCount) {
            val view = binding.llEventColorContainer.getChildAt(i)
            val colorStr = view.tag as String
            val isSelected = selectedEventColor.uppercase() == colorStr.uppercase()
            if (isSelected) anyPredefinedSelected = true
            
            val drawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.parseColor(colorStr))
                if (isSelected) {
                    setStroke((3 * resources.displayMetrics.density).toInt(), android.graphics.Color.DKGRAY)
                } else {
                    setStroke((1 * resources.displayMetrics.density).toInt(), android.graphics.Color.LTGRAY)
                }
            }
            view.background = drawable
            view.scaleX = if (isSelected) 1.15f else 1.0f
            view.scaleY = if (isSelected) 1.15f else 1.0f
        }

        // Si el seleccionado no es predefinido, marcamos el botón de custom
        try {
            binding.cvCustomColor.strokeColor = if (!anyPredefinedSelected) 
                android.graphics.Color.parseColor(selectedEventColor) 
            else 
                android.graphics.Color.parseColor("#DDDDDD")
        } catch (_: Exception) {
            binding.cvCustomColor.strokeColor = android.graphics.Color.parseColor("#DDDDDD")
        }
        
        binding.cvCustomColor.strokeWidth = if (!anyPredefinedSelected) 
            (3 * resources.displayMetrics.density).toInt() 
        else 
            (1 * resources.displayMetrics.density).toInt()
    }

    private fun setupEditMode() {
        binding.tvTitleContainer.text = "Editar Evento"
        binding.btnSaveEvent.text = "GUARDAR CAMBIOS"
        
        // Cargar datos del evento desde el ViewModel si es edición
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadEvent(editingEventId!!)
            viewModel.event.collectLatest { event ->
                event?.let {
                    binding.etTitle.setText(it.name)
                    binding.etDescription.setText(it.description)
                    binding.switchPublic.isChecked = it.visibility == "public"
                    selectedGroupId = it.groupId
                    binding.acEventIcon.setText(it.icon ?: "Evento General", false)
                    selectedEventColor = it.color ?: "#1565C0"
                    it.startDate?.let { date ->
                        startDateTime.time = date
                        binding.etStartDateTime.setText(dateTimeFormatter.format(date))
                    }
                    it.endDate?.let { date ->
                        endDateTime.time = date
                        binding.etEndDateTime.setText(dateTimeFormatter.format(date))
                    }
                    updateEventColorSelectionUI()
                }
            }
        }
        
        // Ocultar sección de roles en edición (se gestionan desde el detalle)
        binding.btnAddRole.visibility = View.GONE
        binding.rvRolesToCreate.visibility = View.GONE
        binding.tvEmptyRolesHint.visibility = View.GONE
        // El separador anterior
        binding.btnAddRole.parent.let { (it as? View)?.visibility = View.GONE }
    }

    private fun saveEvent() {
        val name = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val isPublic = binding.switchPublic.isChecked
        val icon = binding.acEventIcon.text.toString()
        val userId = SupabaseModule.client.auth.currentUserOrNull()?.id

        if (name.isBlank()) {
            binding.etTitle.error = "Title required"
            return
        }

        if (endDateTime.before(startDateTime)) {
            Toast.makeText(context, "La fecha de finalización debe ser posterior a la de inicio", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId == null) {
            Toast.makeText(context, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val visibilityValue = if (isPublic) "public" else "private"
        val groupId = selectedGroupId

        if (visibilityValue == "private" && groupId == null) {
            Toast.makeText(context, "Debes seleccionar un grupo para crear un evento privado", Toast.LENGTH_SHORT).show()
            return
        }

        if (editingEventId != null) {
            // Lógica de Actualización
            val updatedEvent = Event(
                id = editingEventId,
                name = name,
                description = if (description.isEmpty()) null else description,
                visibility = visibilityValue,
                createdBy = userId, 
                startDate = startDateTime.time,
                endDate = endDateTime.time,
                groupId = groupId,
                icon = icon,
                color = selectedEventColor
            )
            viewModel.updateEvent(updatedEvent)
        } else {
            // Lógica de Creación original
            val slug = name.lowercase().replace("[^a-z0-9]".toRegex(), "-").take(50) + "-" + System.currentTimeMillis()
            val event = Event(
                name = name,
                description = if (description.isEmpty()) null else description,
                visibility = visibilityValue,
                createdBy = userId,
                slug = slug,
                startDate = startDateTime.time,
                endDate = endDateTime.time,
                groupId = groupId,
                icon = icon,
                color = selectedEventColor
            )
            viewModel.createEvent(event, createdRoles)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.adminGroups.collectLatest { groups ->
                adminGroups = groups
                val names = groups.map { it.name }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                binding.actvGroupSelector.setAdapter(adapter)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.createEventState.collectLatest { state ->
                when (state) {
                    is EventViewModel.CreateEventState.Loading -> {
                        binding.btnSaveEvent.isEnabled = false
                    }
                    is EventViewModel.CreateEventState.Success -> {
                        val msg = if (editingEventId != null) "Evento actualizado" else "Evento creado correctamente"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                    is EventViewModel.CreateEventState.Error -> {
                        binding.btnSaveEvent.isEnabled = true
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetCreateState()
                    }
                    is EventViewModel.CreateEventState.Idle -> {
                        binding.btnSaveEvent.isEnabled = true
                    }
                }
            }
        }
    }

    private fun showAddRoleDialog() {
        val dialogBinding = DialogAddRoleBinding.inflate(layoutInflater)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // Configurar dropdown de iconos
        val icons = listOf("Logística", "Catering", "Música", "Invitados", "Limpieza", "Fotografía", "Decoración", "Seguridad")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, icons)
        dialogBinding.acRoleIcon.setAdapter(adapter)

        // Configurar selección de color (similar a AddRoleFragment)
        var selectedColor = "#1565C0"
        val colors = listOf("#1565C0", "#1E88E5", "#43A047", "#E53935", "#FB8C00", "#8E24AA")
        dialogBinding.llColorContainer.removeAllViews()
        colors.forEach { colorStr ->
            val colorView = View(requireContext()).apply {
                val size = (32 * resources.displayMetrics.density).toInt()
                layoutParams = ViewGroup.MarginLayoutParams(size, size).apply { setMargins(8, 8, 8, 8) }
                setBackgroundColor(android.graphics.Color.parseColor(colorStr))
                setOnClickListener { selectedColor = colorStr }
            }
            dialogBinding.llColorContainer.addView(colorView)
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val roleName = dialogBinding.etRoleName.text.toString().trim()
            val description = dialogBinding.etRoleTasks.text.toString().trim()
            val min = dialogBinding.etMinPeople.text.toString().toIntOrNull()
            val max = dialogBinding.etMaxPeople.text.toString().toIntOrNull()
            val mandatory = dialogBinding.cbIsMandatory.isChecked
            val icon = dialogBinding.acRoleIcon.text.toString()

            if (roleName.isNotBlank()) {
                val newRole = Role(
                    name = roleName, 
                    description = description,
                    minPeople = min,
                    maxPeople = max,
                    isMandatory = mandatory,
                    icon = icon,
                    color = selectedColor
                )
                createdRoles.add(newRole)
                rolesAdapter.submitList(createdRoles.toList())
                updateRolesVisibility()
                dialog.dismiss()
            } else {
                dialogBinding.etRoleName.error = "Nombre requerido"
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
