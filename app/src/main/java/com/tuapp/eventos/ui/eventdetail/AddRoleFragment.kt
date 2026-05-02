package com.tuapp.eventos.ui.eventdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.tuapp.eventos.databinding.DialogAddRoleBinding
import com.tuapp.eventos.domain.model.Role
import com.tuapp.eventos.ui.events.EventViewModel
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddRoleFragment : DialogFragment() {

    private var _binding: DialogAddRoleBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: EventViewModel by viewModels({ 
        // Si el fragmento es mostrado desde EventDetailFragment o uno de sus hijos
        var parent = requireParentFragment()
        while (parent !is EventDetailFragment && parent.parentFragment != null) {
            parent = parent.requireParentFragment()
        }
        parent
    })
    private var eventId: String? = null
    private var roleToEdit: Role? = null
    private var selectedColor: String = "#1565C0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventId = arguments?.getString("eventId")
        val roleJson = arguments?.getString("roleJson")
        if (roleJson != null) {
            roleToEdit = kotlinx.serialization.json.Json.decodeFromString<Role>(roleJson)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddRoleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.tvDialogTitle.text = if (roleToEdit == null) "Añadir Rol" else "Editar Rol"
        binding.btnConfirm.text = if (roleToEdit == null) "Crear" else "Guardar"

        setupIconDropdown()
        setupColorSelection()
        setupButtons()
        observeViewModel()
        
        roleToEdit?.let { populateFields(it) }
    }

    private fun setupColorSelection() {
        val colors = listOf("#1565C0", "#1E88E5", "#43A047", "#E53935", "#FB8C00", "#8E24AA", "#FDD835")
        binding.llColorContainer.removeAllViews()
        
        colors.forEach { colorStr ->
            val colorView = View(requireContext()).apply {
                val size = (32 * resources.displayMetrics.density).toInt()
                layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                    setMargins(8, 8, 8, 8)
                }
                tag = colorStr
                setOnClickListener {
                    selectedColor = colorStr
                    updateColorSelectionUI()
                }
            }
            binding.llColorContainer.addView(colorView)
        }

        binding.cvCustomColor.setOnClickListener {
            ColorPickerDialog.Builder(requireContext())
                .setTitle("Seleccionar Color")
                .setPreferenceName("MyColorPicker")
                .setPositiveButton("Confirmar", ColorEnvelopeListener { envelope, _ ->
                    selectedColor = "#${envelope.hexCode}"
                    updateColorSelectionUI()
                })
                .setNegativeButton("Cancelar") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .attachAlphaSlideBar(false)
                .attachBrightnessSlideBar(true)
                .show()
        }

        updateColorSelectionUI()
    }

    private fun updateColorSelectionUI() {
        val predefinedColors = listOf("#1565C0", "#1E88E5", "#43A047", "#E53935", "#FB8C00", "#8E24AA", "#FDD835")
        var anyPredefinedSelected = false

        for (i in 0 until binding.llColorContainer.childCount) {
            val view = binding.llColorContainer.getChildAt(i)
            val colorStr = view.tag as String
            val isSelected = selectedColor.uppercase() == colorStr.uppercase()
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
            view.scaleX = if (isSelected) 1.1f else 1.0f
            view.scaleY = if (isSelected) 1.1f else 1.0f
        }

        // Si el seleccionado no es predefinido, marcamos el botón de custom
        try {
            binding.cvCustomColor.strokeColor = if (!anyPredefinedSelected) 
                android.graphics.Color.parseColor(selectedColor) 
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

    private fun populateFields(role: Role) {
        binding.etRoleName.setText(role.name)
        binding.etRoleTasks.setText(role.description)
        binding.etMinPeople.setText(role.minPeople?.toString())
        binding.etMaxPeople.setText(role.maxPeople?.toString())
        binding.cbIsMandatory.isChecked = role.isMandatory
        binding.acRoleIcon.setText(role.icon, false)
        selectedColor = role.color ?: "#1565C0"
        
        updateColorSelectionUI()
    }

    private fun setupIconDropdown() {
        val icons = arrayOf("Logística", "Catering", "Música", "Invitados", "Limpieza", "Fotografía", "Decoración", "Seguridad")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, icons)
        binding.acRoleIcon.setAdapter(adapter)
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnConfirm.setOnClickListener {
            val name = binding.etRoleName.text.toString()
            val description = binding.etRoleTasks.text.toString()
            val minPeople = binding.etMinPeople.text.toString().toIntOrNull()
            val maxPeople = binding.etMaxPeople.text.toString().toIntOrNull()
            val isMandatory = binding.cbIsMandatory.isChecked
            val icon = binding.acRoleIcon.text.toString()
            
            if (name.isBlank()) {
                binding.etRoleName.error = "El nombre es obligatorio"
                return@setOnClickListener
            }

            val role = Role(
                id = roleToEdit?.id,
                eventId = eventId,
                name = name,
                description = description,
                minPeople = minPeople,
                maxPeople = maxPeople,
                isMandatory = isMandatory,
                icon = icon,
                color = selectedColor
            )

            if (roleToEdit == null) {
                viewModel.createRole(role)
            } else {
                viewModel.updateRole(role)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.roleOpState.collectLatest { state ->
                when (state) {
                    is EventViewModel.RoleOpState.Loading -> {
                        binding.btnConfirm.isEnabled = false
                    }
                    is EventViewModel.RoleOpState.Success -> {
                        dismiss()
                    }
                    is EventViewModel.RoleOpState.Error -> {
                        binding.btnConfirm.isEnabled = true
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetRoleOpState()
                    }
                    is EventViewModel.RoleOpState.Idle -> {
                        binding.btnConfirm.isEnabled = true
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        viewModel.resetRoleOpState()
    }

    companion object {
        fun newInstance(eventId: String, role: Role? = null): AddRoleFragment {
            return AddRoleFragment().apply {
                arguments = Bundle().apply {
                    putString("eventId", eventId)
                    role?.let { 
                        putString("roleJson", kotlinx.serialization.json.Json.encodeToString(Role.serializer(), it))
                    }
                }
            }
        }
    }
}
