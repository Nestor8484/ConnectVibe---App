package com.tuapp.eventos.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.tuapp.eventos.databinding.FragmentAddMemberBinding

class AddMemberFragment : Fragment() {

    private var _binding: FragmentAddMemberBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddMemberBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRoleDropdown()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSaveMember.setOnClickListener {
            val email = binding.etMemberEmail.text.toString()
            if (email.isNotEmpty()) {
                // Simulación de guardado
                Snackbar.make(binding.root, "Invitación enviada a $email", Snackbar.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                binding.etMemberEmail.error = "Introduce un email válido"
            }
        }
    }

    private fun setupRoleDropdown() {
        val roles = arrayOf("Administrador", "Miembro", "Observador")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roles)
        binding.actvRole.setAdapter(adapter)
        binding.actvRole.setText(roles[1], false) // Por defecto: Miembro
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}