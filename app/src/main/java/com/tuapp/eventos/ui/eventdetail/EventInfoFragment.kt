package com.tuapp.eventos.ui.eventdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.eventos.R
import com.tuapp.eventos.databinding.FragmentEventInfoBinding
import com.tuapp.eventos.domain.model.MemberRole
import com.tuapp.eventos.ui.events.EventViewModel
import com.tuapp.eventos.di.SupabaseModule
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class EventInfoFragment : Fragment() {

    private var _binding: FragmentEventInfoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventViewModel by activityViewModels()
    private val participantAdapter = ParticipantAdapter(
        onAdminPromotion = { userId, isAdmin ->
            viewModel.event.value?.id?.let { eventId ->
                viewModel.updateParticipantRole(eventId, userId, isAdmin)
            }
        },
        onRemoveMember = { userId ->
            viewModel.event.value?.id?.let { eventId ->
                viewModel.removeParticipant(eventId, userId)
            }
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        
        binding.btnAddParticipant.setOnClickListener {
            viewModel.event.value?.id?.let { eventId ->
                val bundle = Bundle().apply {
                    putString("eventId", eventId)
                }
                findNavController().navigate(R.id.action_global_addParticipantFragment, bundle)
            }
        }

        binding.btnEditEvent.setOnClickListener {
            viewModel.event.value?.let { event ->
                EditEventDialogFragment.newInstance(event).show(childFragmentManager, "EditEventDialogFragment")
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvParticipants.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = participantAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.event, viewModel.participants) { event, participants ->
                event to participants
            }.collectLatest { (event, participants) ->
                event?.let {
                    binding.tvDetailTitle.text = it.name
                    binding.tvDetailDescription.text = it.description ?: "Sin descripción"
                    
                    // Mostrar fechas
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val startStr = it.startDate?.let { date -> dateFormat.format(date) }
                    val endStr = it.endDate?.let { date -> dateFormat.format(date) }
                    
                    when {
                        startStr != null && endStr != null -> {
                            binding.tvDetailDate.text = "$startStr - $endStr"
                            binding.llDateContainer.visibility = View.VISIBLE
                        }
                        startStr != null -> {
                            binding.tvDetailDate.text = startStr
                            binding.llDateContainer.visibility = View.VISIBLE
                        }
                        else -> {
                            binding.llDateContainer.visibility = View.GONE
                        }
                    }

                    val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
                    val isOwner = it.createdBy == userId
                    
                    // Encontrar si el usuario actual es admin según la lista de participantes
                    val currentMember = participants.find { p -> p.userId == userId }
                    val isUserAdmin = currentMember?.role == MemberRole.ADMIN
                    
                    binding.btnEditEvent.visibility = if ((isOwner || isUserAdmin) && it.status == "pending") View.VISIBLE else View.GONE

                    // Aplicar colores dinámicos estilo "Roles"
                    it.color?.let { colorStr ->
                        try {
                            val colorInt = colorStr.toColorInt()
                            val alphaColor = (0.15 * 255).toInt() shl 24 or (colorInt and 0x00FFFFFF)
                            val density = resources.displayMetrics.density

                            // 1. Color del título y botón de editar
                            binding.tvDetailTitle.setTextColor(colorInt)
                            binding.btnEditEvent.iconTint = android.content.res.ColorStateList.valueOf(colorInt)

                            // 2. Contenedor de fecha (fondo suave + borde)
                            val dateBg = binding.llDateContainer.background.mutate() as? android.graphics.drawable.GradientDrawable
                            dateBg?.let { dbg ->
                                dbg.setColor(alphaColor)
                                dbg.setStroke((2 * density).toInt(), colorInt)
                            }
                            binding.tvDetailDate.setTextColor(colorInt)
                            binding.ivCalendarIcon.imageTintList = android.content.res.ColorStateList.valueOf(colorInt)

                            // 3. Card de participantes (fondo suave + borde)
                            binding.rvParticipants.apply {
                                val bg = android.graphics.drawable.GradientDrawable().apply {
                                    setColor(alphaColor)
                                    setStroke((2 * density).toInt(), colorInt)
                                    cornerRadius = 16 * density
                                }
                                background = bg
                            }

                            // 4. Botón Añadir Participante
                            binding.btnAddParticipant.apply {
                                backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)
                                setTextColor(android.graphics.Color.WHITE)
                                iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                            }
                        } catch (e: Exception) {
                            // Color inválido, ignorar
                        }
                    }
                    
                    // Actualizar adaptador con creador e info de admin
                    participantAdapter.submitList(participants)
                    participantAdapter.setEventDetails(it.createdBy, isUserAdmin)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
