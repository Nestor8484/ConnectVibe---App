package com.tuapp.eventos.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.eventos.databinding.FragmentAddMemberBinding
import com.tuapp.eventos.di.SupabaseModule
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddMemberFragment : Fragment() {

    private var _binding: FragmentAddMemberBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MemberViewModel by viewModels()
    private lateinit var searchAdapter: UserSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddMemberBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groupId = arguments?.getString("groupId") ?: ""

        setupRecyclerView(groupId)
        setupSearch()
        observeViewModel()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView(groupId: String) {
        val currentUserId = SupabaseModule.client.auth.currentUserOrNull()?.id ?: ""
        searchAdapter = UserSearchAdapter { profile ->
            viewModel.inviteUser(groupId, profile.id, currentUserId)
        }
        binding.rvUserResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            viewModel.searchUsers(text.toString())
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchState.collectLatest { state ->
                when (state) {
                    is MemberViewModel.SearchState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is MemberViewModel.SearchState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        searchAdapter.submitList(state.users)
                    }
                    is MemberViewModel.SearchState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is MemberViewModel.SearchState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                        searchAdapter.submitList(emptyList())
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.inviteState.collectLatest { state ->
                when (state) {
                    is MemberViewModel.InviteState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is MemberViewModel.InviteState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, "Usuario invitado con éxito", Toast.LENGTH_SHORT).show()
                        viewModel.resetInviteState()
                        findNavController().popBackStack()
                    }
                    is MemberViewModel.InviteState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is MemberViewModel.InviteState.Idle -> {}
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
