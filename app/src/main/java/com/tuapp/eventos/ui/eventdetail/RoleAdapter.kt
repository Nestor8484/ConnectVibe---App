package com.tuapp.eventos.ui.eventdetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.eventos.databinding.ItemRoleBinding
import com.tuapp.eventos.domain.model.Role
import com.tuapp.eventos.domain.model.EventRoleMember
import com.tuapp.eventos.di.SupabaseModule
import io.github.jan.supabase.auth.auth

class RoleAdapter(
    private val onRoleClick: (Role) -> Unit,
    private val onDeleteClick: (Role) -> Unit
) : ListAdapter<Role, RoleAdapter.RoleViewHolder>(RoleDiffCallback()) {

    private var isAdmin: Boolean = false
    private var eventStatus: String = "pending"
    private var roleMembers: List<EventRoleMember> = emptyList()

    fun setAdminStatus(admin: Boolean) {
        if (this.isAdmin != admin) {
            this.isAdmin = admin
            notifyDataSetChanged()
        }
    }

    fun setEventStatus(status: String) {
        if (this.eventStatus != status) {
            this.eventStatus = status
            notifyDataSetChanged()
        }
    }

    fun setRoleMembers(members: List<EventRoleMember>) {
        this.roleMembers = members
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoleViewHolder {
        val binding = ItemRoleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoleViewHolder, position: Int) {
        holder.bind(getItem(position), onRoleClick, onDeleteClick, isAdmin, eventStatus, roleMembers)
    }

    class RoleViewHolder(private val binding: ItemRoleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            role: Role, 
            onRoleClick: (Role) -> Unit, 
            onDeleteClick: (Role) -> Unit, 
            isAdmin: Boolean,
            eventStatus: String,
            roleMembers: List<EventRoleMember>
        ) {
            binding.tvRoleName.text = role.name
            
            // ... (keep icon and color logic)
            val iconRes = when (role.icon ?: role.name) {
                "Logística" -> android.R.drawable.ic_menu_manage
                "Catering" -> android.R.drawable.ic_menu_view
                "Música" -> android.R.drawable.ic_lock_silent_mode_off
                "Invitados" -> android.R.drawable.ic_menu_agenda
                "Limpieza" -> android.R.drawable.ic_menu_delete
                "Fotografía" -> android.R.drawable.ic_menu_camera
                "Decoración" -> android.R.drawable.ic_menu_gallery
                "Seguridad" -> android.R.drawable.ic_lock_lock
                else -> android.R.drawable.ic_menu_help
            }
            binding.ivRoleIcon.setImageResource(iconRes)
            
            role.color?.let {
                val color = android.graphics.Color.parseColor(it)
                binding.ivRoleIcon.imageTintList = android.content.res.ColorStateList.valueOf(color)
                binding.tvRoleName.setTextColor(color)
                
                // Ajustar el color de fondo de la tarjeta con una versión muy clara/transparente
                val alphaColor = (0.1 * 255).toInt() shl 24 or (color and 0x00FFFFFF)
                binding.root.setCardBackgroundColor(alphaColor)
                binding.root.strokeColor = color
            }

            // Dots logic for capacity
            val membersInRole = roleMembers.filter { it.roleId == role.id }
            val count = membersInRole.size
            
            updateDots(count, role)
            
            // Mostrar texto de capacidad (Asignados / Mínimo / Máximo)
            val minPeople = role.minPeople ?: 0
            val maxPeople = role.maxPeople?.toString() ?: "∞"
            val isCapacityReached = count >= minPeople
            
            binding.tvRoleCapacity.text = "$count / $minPeople / $maxPeople"
            
            role.color?.let { colorStr ->
                val roleColor = android.graphics.Color.parseColor(colorStr)
                if (isCapacityReached) {
                    binding.tvRoleCapacity.setTextColor(roleColor)
                } else {
                    // Color grisáceo si no se llega al mínimo (mezclando con gris o bajando opacidad)
                    binding.tvRoleCapacity.setTextColor(android.graphics.Color.GRAY)
                }
            }

            val isEditable = isAdmin && eventStatus == "pending"
            binding.btnDeleteRole.visibility = if (isEditable) View.VISIBLE else View.GONE
            binding.btnDeleteRole.setOnClickListener { onDeleteClick(role) }

            binding.root.setOnClickListener { 
                if (isEditable) onRoleClick(role) 
            }
        }

        private fun updateDots(count: Int, role: Role) {
            binding.llDotsContainer.removeAllViews()
            val context = binding.root.context
            val density = context.resources.displayMetrics.density
            val size = (12 * density).toInt()
            val margin = (8 * density).toInt()

            val min = role.minPeople ?: 0
            val max = role.maxPeople
            val roleColor = role.color?.let { android.graphics.Color.parseColor(it) } ?: android.graphics.Color.GRAY

            var dotsToShow = when {
                max != null -> max
                min > 0 -> if (count >= min) count + 1 else min
                else -> if (count == 0) 1 else count + 1
            }
            
            // Limitamos a 10 puntos visuales
            val finalDotsToShow = if (dotsToShow > 10) 10 else dotsToShow

            for (i in 0 until finalDotsToShow) {
                val dot = View(context).apply {
                    val params = android.widget.LinearLayout.LayoutParams(size, size).apply {
                        setMargins(0, 0, margin, 0)
                    }
                    layoutParams = params

                    val isFilled = i < count
                    val isMandatoryZone = i < min || role.isMandatory

                    val backgroundRes = when {
                        isMandatoryZone && isFilled -> com.tuapp.eventos.R.drawable.bg_dot_mandatory_filled
                        isMandatoryZone && !isFilled -> com.tuapp.eventos.R.drawable.bg_dot_mandatory_outline
                        !isMandatoryZone && isFilled -> com.tuapp.eventos.R.drawable.bg_dot_filled
                        else -> com.tuapp.eventos.R.drawable.bg_dot_outline
                    }
                    
                    // Obtenemos el drawable y lo mutamos para que no afecte a otros roles
                    val shape = androidx.core.content.ContextCompat.getDrawable(context, backgroundRes)?.mutate() as? android.graphics.drawable.GradientDrawable
                    
                    shape?.let { g ->
                        if (isFilled) {
                            // Relleno con el color del rol
                            g.setColor(roleColor)
                            if (isMandatoryZone) {
                                // Borde blanco sutil para los obligatorios llenos
                                g.setStroke((1.5 * density).toInt(), android.graphics.Color.WHITE)
                            } else {
                                g.setStroke(0, 0)
                            }
                        } else {
                            // Fondo transparente y borde con el color del rol
                            g.setColor(android.graphics.Color.TRANSPARENT)
                            val strokeWidth = if (isMandatoryZone) (2.5 * density).toInt() else (1.5 * density).toInt()
                            g.setStroke(strokeWidth, roleColor)
                        }
                    }
                    background = shape
                }
                binding.llDotsContainer.addView(dot)
            }
        }
    }

    class RoleDiffCallback : DiffUtil.ItemCallback<Role>() {
        override fun areItemsTheSame(oldItem: Role, newItem: Role): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Role, newItem: Role): Boolean = oldItem == newItem
    }
}
