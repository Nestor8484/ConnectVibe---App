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
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.eventos.R
import com.tuapp.eventos.databinding.FragmentEventListBinding
import com.tuapp.eventos.di.SupabaseModule
import com.tuapp.eventos.ui.profile.NotificationViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PrivateEventsFragment : Fragment() {

    private var _binding: FragmentEventListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GroupViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()

    private val groupAdapter = GroupAdapter(
        onGroupClick = { group ->
            val bundle = Bundle().apply {
                putString("groupId", group.id)
                putString("groupName", group.name)
            }
            findNavController().navigate(R.id.action_privateEventsFragment_to_groupDetailFragment, bundle)
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        
        binding.tilEventFilter.visibility = View.GONE
        
        val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
        if (userId != null) {
            viewModel.loadGroups(userId)
            notificationViewModel.loadNotifications(userId)
        }

        binding.fabAddEvent.setOnClickListener {
            findNavController().navigate(R.id.action_privateEventsFragment_to_createGroupFragment)
        }

        binding.ivUserProfile.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }
    }

    private fun setupToolbar() {
        binding.tvScreenTitle.text = getString(R.string.private_label)
    }

    private fun setupRecyclerView() {
        binding.rvEvents.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.groupsState.collectLatest { state ->
                when (state) {
                    is GroupViewModel.GroupsState.Loading -> {
                    }
                    is GroupViewModel.GroupsState.Success -> {
                        groupAdapter.submitList(state.groups)
                    }
                    is GroupViewModel.GroupsState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            notificationViewModel.hasPendingNotifications.collectLatest { hasPending ->
                binding.vNotificationBadge.visibility = if (hasPending) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
