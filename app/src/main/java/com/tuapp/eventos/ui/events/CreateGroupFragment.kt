package com.tuapp.eventos.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.tuapp.eventos.R
import com.tuapp.eventos.data.repository.GroupRepository
import com.tuapp.eventos.databinding.FragmentCreateGroupBinding
import com.tuapp.eventos.di.SupabaseModule
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class CreateGroupFragment : Fragment() {

    private var _binding: FragmentCreateGroupBinding? = null
    private val binding get() = _binding!!
    private val groupRepository = GroupRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCreateGroup.setOnClickListener {
            val name = binding.etGroupName.text.toString()
            if (name.isNotEmpty()) {
                createGroup(name)
            } else {
                binding.tilGroupName.error = "El nombre no puede estar vacío"
            }
        }
    }

    private fun createGroup(name: String) {
        val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
        if (userId == null) {
            Toast.makeText(requireContext(), "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnCreateGroup.isEnabled = false

        lifecycleScope.launch {
            val result = groupRepository.createGroup(name, userId)
            binding.progressBar.visibility = View.GONE
            binding.btnCreateGroup.isEnabled = true

            if (result.isSuccess) {
                val groupId = result.getOrNull()
                val bundle = Bundle().apply {
                    putString("groupId", groupId ?: "")
                    putString("groupName", name)
                }
                findNavController().navigate(R.id.action_createGroupFragment_to_groupDetailFragment, bundle)
            } else {
                Toast.makeText(requireContext(), "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
