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
import com.tuapp.eventos.databinding.FragmentAccountSettingsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AccountSettingsFragment : Fragment() {

    private var _binding: FragmentAccountSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountSettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnUpdateProfile.setOnClickListener {
            val fullName = binding.etFullName.text.toString()
            val username = binding.etUsername.text.toString()

            if (fullName.isNotEmpty() && username.isNotEmpty()) {
                viewModel.updateProfile(fullName, username)
            } else {
                Toast.makeText(context, getString(R.string.error_fields_required), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnUpdatePassword.setOnClickListener {
            val newPass = binding.etNewPassword.text.toString()
            val confirmPass = binding.etConfirmPassword.text.toString()

            if (newPass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (newPass == confirmPass) {
                    viewModel.updatePassword(newPass)
                } else {
                    Toast.makeText(context, getString(R.string.error_passwords_dont_match), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, getString(R.string.error_fields_required), Toast.LENGTH_SHORT).show()
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentProfile.collectLatest { profile ->
                profile?.let {
                    binding.etFullName.setText(it.full_name)
                    binding.etUsername.setText(it.username)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is AccountSettingsViewModel.AccountSettingsState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is AccountSettingsViewModel.AccountSettingsState.ProfileUpdateSuccess -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, getString(R.string.profile_updated_success), Toast.LENGTH_SHORT).show()
                        viewModel.resetState()
                    }
                    is AccountSettingsViewModel.AccountSettingsState.PasswordUpdateSuccess -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, getString(R.string.password_updated_success), Toast.LENGTH_SHORT).show()
                        binding.etNewPassword.text?.clear()
                        binding.etConfirmPassword.text?.clear()
                        viewModel.resetState()
                    }
                    is AccountSettingsViewModel.AccountSettingsState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                    else -> {
                        binding.progressBar.visibility = View.GONE
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
