package com.tuapp.eventos.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.eventos.R
import com.tuapp.eventos.databinding.FragmentEventListBinding
import com.tuapp.eventos.domain.model.Event
import java.util.Date

class PrivateEventsFragment : Fragment() {

    private var _binding: FragmentEventListBinding? = null
    private val binding get() = _binding!!

    private val eventAdapter = EventAdapter { group ->
        val bundle = Bundle().apply {
            putString("groupId", group.id)
            putString("groupName", group.title)
        }
        findNavController().navigate(R.id.action_privateEventsFragment_to_groupDetailFragment, bundle)
    }

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
        loadData()
        
        binding.fabAddEvent.visibility = View.GONE

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
            adapter = eventAdapter
        }
    }

    private fun loadData() {
        val dummyGroups = listOf(
            Event("g1", "Los de la Uni", "Amigos de ingeniería", Date(), "Facultad", false, "o1"),
            Event("g2", "Familia Pérez", "Reuniones familiares", Date(), "Casa", false, "o1"),
            Event("g3", "Equipo de Pádel", "Partidos semanales", Date(), "Club Deportivo", false, "o1"),
            Event("g4", "Viaje Verano 2024", "Planificación viaje", Date(), "Destino", false, "o1")
        )
        eventAdapter.submitList(dummyGroups)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
