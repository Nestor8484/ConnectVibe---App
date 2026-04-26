package com.tuapp.eventos.ui.events

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
import com.tuapp.eventos.databinding.DialogAddRoleBinding
import com.tuapp.eventos.databinding.FragmentCreateEventBinding
import com.tuapp.eventos.di.SupabaseModule
import com.tuapp.eventos.domain.model.Event
import com.tuapp.eventos.domain.model.Role
import com.tuapp.eventos.ui.eventdetail.RoleAdapter
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date

class CreateEventFragment : Fragment() {

    private var _binding: FragmentCreateEventBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventViewModel by viewModels()
    private val rolesAdapter = RoleAdapter { /* no-op click */ }
    private val createdRoles = mutableListOf<Role>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groupId = arguments?.getString("groupId")
        if (groupId != null) {
            binding.switchPublic.isChecked = false
            binding.switchPublic.isEnabled = false
            binding.tvVisibilityHint.visibility = View.VISIBLE
            binding.tvVisibilityHint.text = "Los eventos de grupo son siempre privados"
        }

        setupRecyclerView()

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

    private fun saveEvent() {
        val name = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val isPublic = binding.switchPublic.isChecked
        val userId = SupabaseModule.client.auth.currentUserOrNull()?.id

        if (name.isBlank()) {
            binding.etTitle.error = "Title required"
            return
        }

        if (userId == null) {
            Toast.makeText(context, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Generating a unique slug
        val slug = name.lowercase().replace("[^a-z0-9]".toRegex(), "-").take(50) + "-" + System.currentTimeMillis()

        // Using lowercase exactly as confirmed by the user
        val visibilityValue = if (isPublic) "public" else "private"
        val groupId = arguments?.getString("groupId")

        val event = Event(
            name = name,
            description = if (description.isEmpty()) null else description,
            visibility = visibilityValue,
            createdBy = userId,
            slug = slug,
            startDate = Date(),
            endDate = Date(),
            groupId = groupId
        )

        viewModel.createEvent(event, createdRoles)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.createEventState.collectLatest { state ->
                when (state) {
                    is EventViewModel.CreateEventState.Loading -> {
                        binding.btnSaveEvent.isEnabled = false
                    }
                    is EventViewModel.CreateEventState.Success -> {
                        Toast.makeText(context, "Evento creado correctamente", Toast.LENGTH_SHORT).show()
                        viewModel.resetCreateState()
                        findNavController().popBackStack()
                    }
                    is EventViewModel.CreateEventState.Error -> {
                        binding.btnSaveEvent.isEnabled = true
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {
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

        val icons = listOf("Fotógrafo", "DJ", "Seguridad", "Catering", "Limpieza")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, icons)
        dialogBinding.acRoleIcon.setAdapter(adapter)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val roleName = dialogBinding.etRoleName.text.toString().trim()
            if (roleName.isNotBlank()) {
                val newRole = Role(name = roleName, description = "Rol del evento")
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
