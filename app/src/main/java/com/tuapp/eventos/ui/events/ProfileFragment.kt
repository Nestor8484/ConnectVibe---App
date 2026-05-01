package com.tuapp.eventos.ui.events

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
import com.tuapp.eventos.databinding.FragmentProfileBinding
import com.tuapp.eventos.utils.ThemeManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    private lateinit var themeManager: ThemeManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        themeManager = ThemeManager(requireContext())
        
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }

        binding.btnAccountSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_accountSettingsFragment)
        }

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_notificationsFragment)
        }

        setupDarkModeToggle()
        
        observeProfile()
        observeLogout()
        observeNotifications()
    }

    private fun observeNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pendingNotificationsCount.collectLatest { count ->
                if (count > 0) {
                    binding.tvNotificationBadge.visibility = View.VISIBLE
                    binding.tvNotificationBadge.text = count.toString()
                } else {
                    binding.tvNotificationBadge.visibility = View.GONE
                }
            }
        }
    }

    private fun setupDarkModeToggle() {
        binding.switchDarkMode.isChecked = themeManager.isDarkMode()

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            themeManager.setDarkMode(isChecked)
        }
    }

    private fun observeLogout() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.logoutState.collectLatest { state ->
                when (state) {
                    is ProfileViewModel.LogoutState.Loading -> {
                        binding.btnLogout.isEnabled = false
                    }
                    is ProfileViewModel.LogoutState.Success -> {
                        findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
                    }
                    is ProfileViewModel.LogoutState.Error -> {
                        binding.btnLogout.isEnabled = true
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        binding.btnLogout.isEnabled = true
                    }
                }
            }
        }
    }

    private fun observeProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.profileState.collectLatest { state ->
                when (state) {
                    is ProfileViewModel.ProfileState.Loading -> {
                    }
                    is ProfileViewModel.ProfileState.Success -> {
                        binding.tvUserName.text = state.profile.full_name ?: state.profile.username ?: "Usuario"
                        binding.tvUserEmail.text = state.email
                    }
                    is ProfileViewModel.ProfileState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
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
