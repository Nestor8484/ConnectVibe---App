package com.tuapp.eventos.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.eventos.databinding.FragmentNotificationsBinding
import com.tuapp.eventos.di.SupabaseModule
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationViewModel by viewModels()
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = SupabaseModule.client.auth.currentUserOrNull()?.id ?: ""

        setupRecyclerView(userId)
        observeViewModel()
        
        viewModel.loadNotifications(userId)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView(userId: String) {
        notificationAdapter = NotificationAdapter(
            onAccept = { notification ->
                viewModel.acceptInvitation(notification, userId)
            },
            onDecline = { notification ->
                viewModel.deleteNotification(notification, userId)
            },
            onDelete = { notification ->
                viewModel.deleteNotification(notification, userId)
            }
        )
        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notificationAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notificationsState.collectLatest { state ->
                when (state) {
                    is NotificationViewModel.NotificationsState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.tvEmptyState.visibility = View.GONE
                    }
                    is NotificationViewModel.NotificationsState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        notificationAdapter.submitList(state.notifications)
                        binding.tvEmptyState.visibility = if (state.notifications.isEmpty()) View.VISIBLE else View.GONE
                    }
                    is NotificationViewModel.NotificationsState.Error -> {
                        binding.progressBar.visibility = View.GONE
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
