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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddRoleFragment : DialogFragment() {

    private var _binding: DialogAddRoleBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: EventViewModel by viewModels({ requireParentFragment() })
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
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
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
                setBackgroundColor(android.graphics.Color.parseColor(colorStr))
                setOnClickListener {
                    selectedColor = colorStr
                    updateColorSelectionUI()
                }
            }
            binding.llColorContainer.addView(colorView)
        }
        updateColorSelectionUI()
    }

    private fun updateColorSelectionUI() {
        for (i in 0 until binding.llColorContainer.childCount) {
            val view = binding.llColorContainer.getChildAt(i)






















            val colorStr = view.tag as String
            val isSelected = selectedColor == colorStr
            
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
                        // Importante: No resetear el estado aquí si otros observadores lo necesitan
                        // O hacerlo después de cerrar
                        dismiss()
                    }
                    is EventViewModel.RoleOpState.Error -> {
                        binding.btnConfirm.isEnabled = true
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetRoleOpState()
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
