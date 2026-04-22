package com.tuapp.eventos.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.tuapp.eventos.databinding.DialogAddRoleBinding
import com.tuapp.eventos.databinding.FragmentCreateEventBinding

class CreateEventFragment : Fragment() {

    private var _binding: FragmentCreateEventBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnAddRole.setOnClickListener {
            showAddRoleDialog()
        }

        binding.btnSaveEvent.setOnClickListener {
            val title = binding.etTitle.text.toString()
            if (title.isNotBlank()) {
                Toast.makeText(context, "Event $title saved", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                binding.etTitle.error = "Title required"
            }
        }
    }

    private fun showAddRoleDialog() {
        val dialogBinding = DialogAddRoleBinding.inflate(layoutInflater)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // Configurar dropdown de iconos (ejemplo)
        val icons = listOf("Fotógrafo", "DJ", "Seguridad", "Catering", "Limpieza")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, icons)
        dialogBinding.acRoleIcon.setAdapter(adapter)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val roleName = dialogBinding.etRoleName.text.toString()
            if (roleName.isNotBlank()) {
                // Aquí añadirías el rol a la lista del RecyclerView
                Toast.makeText(context, "Rol $roleName añadido", Toast.LENGTH_SHORT).show()
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
