package com.tuapp.eventos.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.eventos.R
import com.tuapp.eventos.databinding.FragmentGroupDetailBinding
import com.tuapp.eventos.domain.model.Event
import java.util.Date

class GroupDetailFragment : Fragment() {

    private var _binding: FragmentGroupDetailBinding? = null
    private val binding get() = _binding!!

    private val eventAdapter = EventAdapter { event ->
        val bundle = Bundle().apply {
            putString("eventId", event.id)
            putString("eventTitle", event.title)
            putString("eventDescription", event.description)
        }
        findNavController().navigate(R.id.action_global_eventDetailFragment, bundle)
    }

    private val memberAdapter: MemberAdapter by lazy {
        MemberAdapter(isAdmin = true) { member ->
            val currentList = memberAdapter.currentList.toMutableList()
            currentList.remove(member)
            memberAdapter.submitList(currentList)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val groupName = arguments?.getString("groupName") ?: "Grupo"
        binding.tvGroupName.text = groupName

        setupRecyclerViews()
        setupTabs()
        setupDashboardToggle()
        loadGroupData()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.ivUserProfile.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        binding.fabAddGroupEvent.setOnClickListener {
            findNavController().navigate(R.id.action_global_createEventFragment)
        }

        binding.includeMembers.fabAddMember.setOnClickListener {
            findNavController().navigate(R.id.action_global_addMemberFragment)
        }
    }

    private fun setupDashboardToggle() {
        binding.includeDashboard.btnToggleGroupChartType.setOnClickListener {
            val isPieVisible = binding.includeDashboard.viewGroupPieChart.visibility == View.VISIBLE
            if (isPieVisible) {
                binding.includeDashboard.viewGroupPieChart.visibility = View.GONE
                binding.includeDashboard.viewGroupBarChart.visibility = View.VISIBLE
                binding.includeDashboard.btnToggleGroupChartType.text = getString(R.string.view_pie_chart)
            } else {
                binding.includeDashboard.viewGroupPieChart.visibility = View.VISIBLE
                binding.includeDashboard.viewGroupBarChart.visibility = View.GONE
                binding.includeDashboard.btnToggleGroupChartType.text = getString(R.string.view_bar_chart)
            }
        }
    }

    private fun setupRecyclerViews() {
        binding.rvGroupEvents.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = eventAdapter
        }

        binding.includeMembers.rvMembers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = memberAdapter
        }
    }

    private fun setupTabs() {
        binding.groupTabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showEvents()
                    1 -> showDashboard()
                    2 -> showMembers()
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun showEvents() {
        binding.rvGroupEvents.visibility = View.VISIBLE
        binding.includeDashboard.root.visibility = View.GONE
        binding.includeMembers.root.visibility = View.GONE
        binding.fabAddGroupEvent.visibility = View.VISIBLE
    }

    private fun showDashboard() {
        binding.rvGroupEvents.visibility = View.GONE
        binding.includeDashboard.root.visibility = View.VISIBLE
        binding.includeMembers.root.visibility = View.GONE
        binding.fabAddGroupEvent.visibility = View.GONE
    }

    private fun showMembers() {
        binding.rvGroupEvents.visibility = View.GONE
        binding.includeDashboard.root.visibility = View.GONE
        binding.includeMembers.root.visibility = View.VISIBLE
        binding.fabAddGroupEvent.visibility = View.GONE
    }

    private fun loadGroupData() {
        loadGroupEvents()
        loadGroupMembers()
    }

    private fun loadGroupEvents() {
        val dummyEvents = listOf(
            Event("4", "Cena de Navidad", "Solo para el grupo", Date(), "Restaurante", false, "o1"),
            Event("5", "Pádel Semanal", "Reserva pista 3", Date(), "Club Deportivo", false, "o1")
        )
        eventAdapter.submitList(dummyEvents)
    }

    private fun loadGroupMembers() {
        val dummyMembers = listOf(
            com.tuapp.eventos.domain.model.GroupMember("u1", "Joan Doe", com.tuapp.eventos.domain.model.MemberRole.ADMIN, "joan@example.com"),
            com.tuapp.eventos.domain.model.GroupMember("u2", "Ana García", com.tuapp.eventos.domain.model.MemberRole.MEMBER, "ana@example.com"),
            com.tuapp.eventos.domain.model.GroupMember("u3", "Carlos Ruiz", com.tuapp.eventos.domain.model.MemberRole.MEMBER, "carlos@example.com")
        )
        memberAdapter.submitList(dummyMembers)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
