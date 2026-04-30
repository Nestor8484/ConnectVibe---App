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
import com.tuapp.eventos.databinding.FragmentEditGroupBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EditGroupFragment : Fragment() {

    private var _binding: FragmentEditGroupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GroupViewModel by viewModels()
    private var selectedGroupColor: String = "#1565C0"
    private var groupId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupId = arguments?.getString("groupId") ?: ""
        if (groupId.isEmpty()) {
            findNavController().popBackStack()
            return
        }

        setupIconDropdown()
        setupColorSelection()
        observeViewModel()
        
        viewModel.loadGroup(groupId)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnUpdateGroup.setOnClickListener {
            updateGroup()
        }
    }

    private fun setupIconDropdown() {
        val icons = arrayOf("Grupo de Amigos", "Familia", "Trabajo", "Deportes", "Estudios", "Viajes", "Hobby")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, icons)
        binding.acGroupIcon.setAdapter(adapter)
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
    }

    private fun updateColorSelectionUI() {
        for (i in 0 until binding.llGroupColorContainer.childCount) {
            val view = binding.llGroupColorContainer.getChildAt(i)
            val colorStr = view.tag as String
            val isSelected = selectedGroupColor == colorStr
            
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
        
        // Actualizar UI general con el color seleccionado
        try {
            val colorInt = android.graphics.Color.parseColor(selectedGroupColor)
            binding.btnUpdateGroup.backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)
            binding.btnBack.imageTintList = android.content.res.ColorStateList.valueOf(colorInt)
        } catch (e: Exception) {}
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentGroup.collectLatest { group ->
                group?.let {
                    binding.etGroupName.setText(it.name)
                    binding.etGroupDescription.setText(it.description)
                    binding.acGroupIcon.setText(it.icon, false)
                    selectedGroupColor = it.color ?: "#1565C0"
                    updateColorSelectionUI()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateState.collectLatest { result ->
                result?.let {
                    binding.progressBar.visibility = View.GONE
                    binding.btnUpdateGroup.isEnabled = true
                    
                    if (it.isSuccess) {
                        Toast.makeText(requireContext(), "Grupo actualizado", Toast.LENGTH_SHORT).show()
                        viewModel.resetUpdateState()
                        findNavController().popBackStack()
                    } else {
                        Toast.makeText(requireContext(), "Error: ${it.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                        viewModel.resetUpdateState()
                    }
                }
            }
        }
    }

    private fun updateGroup() {
        val name = binding.etGroupName.text.toString().trim()
        val description = binding.etGroupDescription.text.toString().trim()
        val icon = binding.acGroupIcon.text.toString()

        if (name.isEmpty()) {
            binding.etGroupName.error = "El nombre es obligatorio"
            return
        }

        binding.btnUpdateGroup.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        viewModel.updateGroup(groupId, name, description, icon, selectedGroupColor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
