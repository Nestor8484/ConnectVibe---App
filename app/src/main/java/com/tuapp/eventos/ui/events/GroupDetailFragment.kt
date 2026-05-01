package com.tuapp.eventos.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.eventos.R
import com.tuapp.eventos.data.model.GroupMember
import com.tuapp.eventos.data.model.Profile
import com.tuapp.eventos.data.repository.GroupRepository
import com.tuapp.eventos.databinding.FragmentGroupDetailBinding
import com.tuapp.eventos.di.SupabaseModule
import com.tuapp.eventos.domain.model.Event
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date

class GroupDetailFragment : Fragment() {

    private var _binding: FragmentGroupDetailBinding? = null
    private val binding get() = _binding!!

    private val eventViewModel: EventViewModel by viewModels()
    private val memberViewModel: MemberViewModel by viewModels()
    private val groupViewModel: GroupViewModel by viewModels()
    private val groupRepository = GroupRepository()

    private val eventAdapter = EventAdapter(
        onEventClick = { event ->
            val bundle = Bundle().apply {
                putString("eventId", event.id)
                putString("eventTitle", event.name)
                putString("eventDescription", event.description)
            }
            findNavController().navigate(R.id.action_global_eventDetailFragment, bundle)
        }
    )

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
        
        val groupId = arguments?.getString("groupId") ?: ""
        val groupName = arguments?.getString("groupName") ?: "Grupo"
        binding.tvGroupName.text = groupName

        setupRecyclerViews()
        setupTabs()
        setupDashboardToggle()
        observeViewModel()
        loadGroupData(groupId)
        checkAdminStatus(groupId)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.ivUserProfile.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        binding.fabAddGroupEvent.setOnClickListener {
            val bundle = Bundle().apply {
                putString("groupId", groupId)
            }
            findNavController().navigate(R.id.createEventFragment, bundle)
        }

        binding.includeMembers.fabAddMember.setOnClickListener {
            val bundle = Bundle().apply {
                putString("groupId", groupId)
            }
            findNavController().navigate(R.id.addMemberFragment, bundle)
        }

        binding.includeInfo.btnEditGroup.setOnClickListener {
            val bundle = Bundle().apply {
                putString("groupId", groupId)
            }
            findNavController().navigate(R.id.action_groupDetailFragment_to_editGroupFragment, bundle)
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
                    3 -> showInfo()
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
        binding.includeInfo.root.visibility = View.GONE
        binding.fabAddGroupEvent.visibility = View.VISIBLE
    }

    private fun showDashboard() {
        binding.rvGroupEvents.visibility = View.GONE
        binding.includeDashboard.root.visibility = View.VISIBLE
        binding.includeMembers.root.visibility = View.GONE
        binding.includeInfo.root.visibility = View.GONE
        binding.fabAddGroupEvent.visibility = View.GONE
    }

    private fun showMembers() {
        binding.rvGroupEvents.visibility = View.GONE
        binding.includeDashboard.root.visibility = View.GONE
        binding.includeMembers.root.visibility = View.VISIBLE
        binding.includeInfo.root.visibility = View.GONE
        binding.fabAddGroupEvent.visibility = View.GONE
    }

    private fun showInfo() {
        binding.rvGroupEvents.visibility = View.GONE
        binding.includeDashboard.root.visibility = View.GONE
        binding.includeMembers.root.visibility = View.GONE
        binding.includeInfo.root.visibility = View.VISIBLE
        binding.fabAddGroupEvent.visibility = View.GONE
        
        // Cargar datos en la vista de info
        groupViewModel.currentGroup.value?.let { group ->
            binding.includeInfo.tvGroupDescription.text = group.description ?: "Sin descripción"
            binding.includeInfo.tvGroupIconName.text = group.icon ?: "Grupo de Amigos"
            
            val iconRes = when (group.icon) {
                "Grupo de Amigos" -> R.drawable.ic_groups
                else -> android.R.drawable.ic_menu_myplaces
            }
            binding.includeInfo.ivGroupInfoIcon.setImageResource(iconRes)
            
            group.color?.let { colorStr ->
                try {
                    val colorInt = android.graphics.Color.parseColor(colorStr)
                    binding.includeInfo.vGroupColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)
                    binding.includeInfo.tvInfoTitle.setTextColor(colorInt)
                    binding.includeInfo.ivGroupInfoIcon.imageTintList = android.content.res.ColorStateList.valueOf(colorInt)
                } catch (e: Exception) {}
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            eventViewModel.eventsState.collectLatest { state ->
                when (state) {
                    is EventViewModel.EventsState.Loading -> {
                    }
                    is EventViewModel.EventsState.Success -> {
                        eventAdapter.submitList(state.events)
                    }
                    is EventViewModel.EventsState.Error -> {
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            memberViewModel.membersState.collectLatest { state ->
                when (state) {
                    is MemberViewModel.MembersState.Loading -> {
                    }
                    is MemberViewModel.MembersState.Success -> {
                        android.util.Log.d("GroupDetail", "Received ${state.members.size} members")
                        memberAdapter.submitList(state.members)
                        if (state.members.isEmpty()) {
                            android.widget.Toast.makeText(context, "No hay miembros en este grupo", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    is MemberViewModel.MembersState.Error -> {
                        android.widget.Toast.makeText(context, state.message, android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            groupViewModel.currentGroup.collectLatest { group ->
                group?.let {
                    binding.tvGroupName.text = it.name
                    it.color?.let { color -> applyGroupStyle(color) }
                }
            }
        }
    }

    private fun applyGroupStyle(colorStr: String) {
        try {
            val colorInt = android.graphics.Color.parseColor(colorStr)
            val alphaColor = (0.15 * 255).toInt() shl 24 or (colorInt and 0x00FFFFFF)
            val density = resources.displayMetrics.density

            // 1. Título (Fondo suave + borde intenso)
            val titleBg = binding.tvGroupName.background?.mutate() as? android.graphics.drawable.GradientDrawable
            titleBg?.let {
                it.setColor(alphaColor)
                it.setStroke((3 * density).toInt(), colorInt)
            }
            binding.tvGroupName.setTextColor(colorInt)

            // 2. Tabs
            binding.groupTabLayout.setSelectedTabIndicatorColor(colorInt)
            binding.groupTabLayout.setTabTextColors(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant),
                colorInt
            )

            // 3. FAB
            binding.fabAddGroupEvent.backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)
            
            // 4. Back button y Profile icon
            binding.btnBack.imageTintList = android.content.res.ColorStateList.valueOf(colorInt)
            binding.ivUserProfile.imageTintList = android.content.res.ColorStateList.valueOf(colorInt)

            // 5. Aplicar a la pestaña de info
            binding.includeInfo.tvInfoTitle.setTextColor(colorInt)
            binding.includeInfo.vGroupColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)
            binding.includeInfo.ivGroupInfoIcon.imageTintList = android.content.res.ColorStateList.valueOf(colorInt)

        } catch (e: Exception) {
            android.util.Log.e("GroupDetail", "Error applying color: ${e.message}")
        }
    }

    private fun loadGroupData(groupId: String) {
        groupViewModel.loadGroup(groupId)
        eventViewModel.loadEventsByGroup(groupId)
        memberViewModel.loadMembers(groupId)
    }

    private fun checkAdminStatus(groupId: String) {
        val userId = SupabaseModule.client.auth.currentUserOrNull()?.id ?: return
        lifecycleScope.launch {
            val isAdmin = groupRepository.isUserAdmin(groupId, userId)
            if (isAdmin) {
                binding.fabAddGroupEvent.visibility = View.VISIBLE
                binding.includeInfo.btnEditGroup.visibility = View.VISIBLE
            } else {
                binding.fabAddGroupEvent.visibility = View.GONE
                binding.includeInfo.btnEditGroup.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
