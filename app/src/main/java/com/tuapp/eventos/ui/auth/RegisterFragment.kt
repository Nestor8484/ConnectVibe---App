package com.tuapp.eventos.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.tuapp.eventos.R
import com.tuapp.eventos.databinding.FragmentRegisterBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString()
            val username = binding.etUsername.text.toString()
            val email = binding.etEmail.text.toString()
            val pass = binding.etPassword.text.toString()

            if (fullName.isNotEmpty() && username.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty()) {
                viewModel.register(email, pass, fullName, username)
            } else {
                Toast.makeText(context, getString(R.string.error_fields_required), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnToLogin.setOnClickListener {
            findNavController().popBackStack()
        }

        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.registrationState.collectLatest { state ->
                when (state) {
                    is RegisterViewModel.RegistrationState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnRegister.isEnabled = false
                    }
                    is RegisterViewModel.RegistrationState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_registerFragment_to_joinedEventsFragment)
                    }
                    is RegisterViewModel.RegistrationState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnRegister.isEnabled = true
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnRegister.isEnabled = true
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
