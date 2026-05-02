package com.tuapp.eventos.ui.eventdetail

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.tuapp.eventos.databinding.DialogEditEventBinding
import com.tuapp.eventos.domain.model.Event
import com.tuapp.eventos.ui.events.EventViewModel
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditEventDialogFragment : DialogFragment() {

    private var _binding: DialogEditEventBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventViewModel by viewModels({ requireParentFragment() })
    private var event: Event? = null
    private var selectedColor: String = "#1565C0"
    private var startDateTime: Calendar = Calendar.getInstance()
    private var endDateTime: Calendar = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
    private val dateTimeFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val eventJson = arguments?.getString("eventJson")
        if (eventJson != null) {
            event = kotlinx.serialization.json.Json.decodeFromString<Event>(eventJson)
            selectedColor = event?.color ?: "#1565C0"
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
        _binding = DialogEditEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        applyEventColor()
        setupIconDropdown()
        setupColorSelection()
        setupDateTimePicker()
        setupButtons()
        observeViewModel()
        
        event?.let { populateFields(it) }
    }

    private fun applyEventColor() {
        val colorStr = viewModel.event.value?.color
        colorStr?.let {
            try {
                val colorInt = android.graphics.Color.parseColor(it)
                binding.root.strokeColor = colorInt
                binding.root.strokeWidth = (2 * resources.displayMetrics.density).toInt()
                
                binding.tvDialogTitle.setTextColor(colorInt)
                binding.btnSave.backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)
                
                val parent = binding.tvDialogTitle.parent as? ViewGroup
                parent?.let { container ->
                    for (i in 0 until container.childCount) {
                        val child = container.getChildAt(i)
                        if (child is com.google.android.material.textfield.TextInputLayout) {
                            child.boxStrokeColor = colorInt
                            child.setEndIconTintList(android.content.res.ColorStateList.valueOf(colorInt))
                            child.defaultHintTextColor = android.content.res.ColorStateList.valueOf(colorInt)
                        }
                    }
                }
            } catch (e: Exception) {}
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

    private fun setupIconDropdown() {
        val icons = arrayOf("Fiesta", "Deporte", "Comida", "Reunión", "Viaje", "Estudio", "Cine", "Otro")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, icons)
        binding.acEventIcon.setAdapter(adapter)
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
                .setPreferenceName("EventColorPicker")
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

    private fun populateFields(event: Event) {
        binding.etTitle.setText(event.name)
        binding.etDescription.setText(event.description)
        binding.etLocation.setText(event.location)
        binding.acEventIcon.setText(event.icon, false)
        selectedColor = event.color ?: "#1565C0"
        event.startDate?.let {
            startDateTime.time = it
            binding.etStartDateTime.setText(dateTimeFormatter.format(it))
        }
        event.endDate?.let {
            endDateTime.time = it
            binding.etEndDateTime.setText(dateTimeFormatter.format(it))
        }
        updateColorSelectionUI()
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val description = binding.etDescription.text.toString()
            val location = binding.etLocation.text.toString()
            val icon = binding.acEventIcon.text.toString()
            
            if (title.isBlank()) {
                binding.etTitle.error = "El título es obligatorio"
                return@setOnClickListener
            }

            if (endDateTime.before(startDateTime)) {
                Toast.makeText(context, "La fecha de finalización debe ser posterior a la de inicio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedEvent = event?.copy(
                name = title,
                description = description,
                location = location,
                icon = icon,
                color = selectedColor,
                startDate = startDateTime.time,
                endDate = endDateTime.time
            )

            if (updatedEvent != null) {
                viewModel.updateEvent(updatedEvent)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.createEventState.collectLatest { state ->
                when (state) {
                    is EventViewModel.CreateEventState.Loading -> {
                        binding.btnSave.isEnabled = false
                    }
                    is EventViewModel.CreateEventState.Success -> {
                        dismiss()
                    }
                    is EventViewModel.CreateEventState.Error -> {
                        binding.btnSave.isEnabled = true
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetCreateState()
                    }
                    is EventViewModel.CreateEventState.Idle -> {
                        binding.btnSave.isEnabled = true
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
        viewModel.resetCreateState()
    }

    companion object {
        fun newInstance(event: Event): EditEventDialogFragment {
            return EditEventDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("eventJson", kotlinx.serialization.json.Json.encodeToString(Event.serializer(), event))
                }
            }
        }
    }
}
