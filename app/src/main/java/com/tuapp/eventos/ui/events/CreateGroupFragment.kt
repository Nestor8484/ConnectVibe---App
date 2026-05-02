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
import com.tuapp.eventos.R
import com.tuapp.eventos.databinding.FragmentCreateGroupBinding
import com.tuapp.eventos.di.SupabaseModule
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class CreateGroupFragment : Fragment() {

    private var _binding: FragmentCreateGroupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GroupViewModel by viewModels()
    private var selectedGroupColor: String = "#1565C0"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupIconDropdown()
        setupColorSelection()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnCreateGroup.setOnClickListener {
            createGroup()
        }
    }

    private fun setupIconDropdown() {
        val icons = arrayOf("Grupo de Amigos", "Familia", "Trabajo", "Deportes", "Estudios", "Viajes", "Hobby")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, icons)
        binding.acGroupIcon.setAdapter(adapter)
        binding.acGroupIcon.setText("Grupo de Amigos", false)
    }

    private fun setupColorSelection() {
        val colors = listOf("#1565C0", "#2E7D32", "#C62828", "#F9A825", "#6A1B9A", "#EF6C00", "#00838F", "#37474F")
        binding.llGroupColorContainer.removeAllViews()
        
        colors.forEach { colorStr ->
            val colorView = View(requireContext()).apply {
                val size = (36 * resources.displayMetrics.density).toInt()
                layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                    setMargins(8, 8, 8, 8)
                }
                tag = colorStr
                setOnClickListener {
                    selectedGroupColor = colorStr
                    updateColorSelectionUI()
                }
            }
            binding.llGroupColorContainer.addView(colorView)
        }

        binding.cvCustomColor.setOnClickListener {
            ColorPickerDialog.Builder(requireContext())
                .setTitle("Seleccionar Color")
                .setPreferenceName("GroupColorPicker")
                .setPositiveButton("Confirmar", ColorEnvelopeListener { envelope, _ ->
                    selectedGroupColor = "#${envelope.hexCode}"
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
        val predefinedColors = listOf("#1565C0", "#2E7D32", "#C62828", "#F9A825", "#6A1B9A", "#EF6C00", "#00838F", "#37474F")
        var anyPredefinedSelected = false

        for (i in 0 until binding.llGroupColorContainer.childCount) {
            val view = binding.llGroupColorContainer.getChildAt(i)
            val colorStr = view.tag as String
            val isSelected = selectedGroupColor.uppercase() == colorStr.uppercase()
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
                android.graphics.Color.parseColor(selectedGroupColor) 
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

    private fun createGroup() {
        val name = binding.etGroupName.text.toString().trim()
        val description = binding.etGroupDescription.text.toString().trim()
        val icon = binding.acGroupIcon.text.toString()
        val userId = SupabaseModule.client.auth.currentUserOrNull()?.id

        if (name.isEmpty()) {
            binding.etGroupName.error = "El nombre es obligatorio"
            return
        }

        if (userId == null) return

        binding.btnCreateGroup.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val repository = com.tuapp.eventos.data.repository.GroupRepository()
            val result = repository.createGroup(name, description, icon, selectedGroupColor, userId)
            
            binding.btnCreateGroup.isEnabled = true
            binding.progressBar.visibility = View.GONE

            if (result.isSuccess) {
                Toast.makeText(requireContext(), "Grupo creado con éxito", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
