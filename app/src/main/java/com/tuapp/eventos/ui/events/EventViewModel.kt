package com.tuapp.eventos.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.eventos.data.repository.EventRepository
import com.tuapp.eventos.data.repository.EventRepositoryImpl
import com.tuapp.eventos.domain.model.*
import com.tuapp.eventos.domain.model.EventRoleMember
import com.tuapp.eventos.di.SupabaseModule
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventViewModel : ViewModel() {

    private val repository: EventRepository = EventRepositoryImpl()
    private val groupRepository = com.tuapp.eventos.data.repository.GroupRepository()

    private val _eventsState = MutableStateFlow<EventsState>(EventsState.Loading)
    val eventsState: StateFlow<EventsState> = _eventsState.asStateFlow()

    private val _createEventState = MutableStateFlow<CreateEventState>(CreateEventState.Idle)
    val createEventState: StateFlow<CreateEventState> = _createEventState.asStateFlow()

    private val _joinEventState = MutableStateFlow<JoinEventState>(JoinEventState.Idle)
    val joinEventState: StateFlow<JoinEventState> = _joinEventState.asStateFlow()

    private val _isParticipating = MutableStateFlow<Boolean>(false)
    val isParticipating: StateFlow<Boolean> = _isParticipating.asStateFlow()

    private val _participants = MutableStateFlow<List<GroupMember>>(emptyList())
    val participants: StateFlow<List<GroupMember>> = _participants.asStateFlow()

    private val _roles = MutableStateFlow<List<Role>>(emptyList())
    val roles: StateFlow<List<Role>> = _roles.asStateFlow()

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    private val _roleOpState = MutableStateFlow<RoleOpState>(RoleOpState.Idle)
    val roleOpState: StateFlow<RoleOpState> = _roleOpState.asStateFlow()

    private val _expenseOpState = MutableStateFlow<RoleOpState>(RoleOpState.Idle)
    val expenseOpState: StateFlow<RoleOpState> = _expenseOpState.asStateFlow()

    private val _roleMembers = MutableStateFlow<List<EventRoleMember>>(emptyList())
    val roleMembers: StateFlow<List<EventRoleMember>> = _roleMembers.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _tasks = MutableStateFlow<List<com.tuapp.eventos.domain.model.EventTask>>(emptyList())
    val tasks: StateFlow<List<com.tuapp.eventos.domain.model.EventTask>> = _tasks.asStateFlow()

    private val _adminGroups = MutableStateFlow<List<com.tuapp.eventos.data.model.Group>>(emptyList())
    val adminGroups: StateFlow<List<com.tuapp.eventos.data.model.Group>> = _adminGroups.asStateFlow()

    fun loadAdminGroups(userId: String) {
        viewModelScope.launch {
            val result = groupRepository.getAdminGroupsForUser(userId)
            if (result.isSuccess) {
                _adminGroups.value = result.getOrDefault(emptyList())
            }
        }
    }

    fun loadPublicEvents(currentUserId: String?) {
        viewModelScope.launch {
            _eventsState.value = EventsState.Loading
            repository.getEvents().collect { allEvents ->
                val publicEvents = allEvents.filter { it.visibility == "public" }
                
                if (currentUserId != null) {
                    val participatingResult = repository.getParticipatingEventIds(currentUserId)
                    if (participatingResult.isSuccess) {
                        val joinedIds = participatingResult.getOrThrow()
                        val updatedEvents = publicEvents.map { 
                            it.copy(isUserParticipating = joinedIds.contains(it.id))
                        }
                        _eventsState.value = EventsState.Success(updatedEvents)
                    } else {
                        _eventsState.value = EventsState.Success(publicEvents)
                    }
                } else {
                    _eventsState.value = EventsState.Success(publicEvents)
                }
            }
        }
    }

    fun loadJoinedEvents(userId: String) {
        viewModelScope.launch {
            _eventsState.value = EventsState.Loading
            val participatingResult = repository.getParticipatingEventIds(userId)
            val joinedIds = if (participatingResult.isSuccess) participatingResult.getOrThrow() else emptyList()

            repository.getEvents().collect { allEvents ->
                val joinedEvents = allEvents.filter { it.createdBy == userId || joinedIds.contains(it.id) }
                    .map { it.copy(isUserParticipating = true) }
                _eventsState.value = EventsState.Success(joinedEvents)
            }
        }
    }

    fun loadEventsByGroup(groupId: String) {
        viewModelScope.launch {
            _eventsState.value = EventsState.Loading
            val userId = SupabaseModule.client.auth.currentUserOrNull()?.id
            
            repository.getEventsByGroup(groupId).collect { events ->
                if (userId != null) {
                    val participatingResult = repository.getParticipatingEventIds(userId)
                    if (participatingResult.isSuccess) {
                        val joinedIds = participatingResult.getOrThrow()
                        val updatedEvents = events.map { 
                            it.copy(isUserParticipating = joinedIds.contains(it.id))
                        }
                        _eventsState.value = EventsState.Success(updatedEvents)
                    } else {
                        _eventsState.value = EventsState.Success(events)
                    }
                } else {
                    _eventsState.value = EventsState.Success(events)
                }
            }
        }
    }

    fun loadParticipants(eventId: String) {
        viewModelScope.launch {
            val result = repository.getEventParticipants(eventId)
            if (result.isSuccess) {
                _participants.value = result.getOrThrow()
            }
        }
    }

    fun updateParticipantRole(eventId: String, userId: String, isAdmin: Boolean) {
        viewModelScope.launch {
            _roleOpState.value = RoleOpState.Loading
            val result = repository.updateParticipantRole(eventId, userId, isAdmin)
            if (result.isSuccess) {
                _roleOpState.value = RoleOpState.Success
                loadParticipants(eventId)
            } else {
                _roleOpState.value = RoleOpState.Error(result.exceptionOrNull()?.message ?: "Error al actualizar rol")
            }
        }
    }

    fun removeParticipant(eventId: String, userId: String) {
        viewModelScope.launch {
            _roleOpState.value = RoleOpState.Loading
            val result = repository.leaveEvent(eventId, userId)
            if (result.isSuccess) {
                _roleOpState.value = RoleOpState.Success
                loadParticipants(eventId)
            } else {
                _roleOpState.value = RoleOpState.Error(result.exceptionOrNull()?.message ?: "Error al eliminar participante")
            }
        }
    }

    fun loadRoles(eventId: String) {
        viewModelScope.launch {
            val result = repository.getRoles(eventId)
            if (result.isSuccess) {
                _roles.value = result.getOrThrow()
            }
            loadRoleMembers(eventId)
        }
    }

    fun loadRoleMembers(eventId: String) {
        viewModelScope.launch {
            val result = repository.getRoleMembers(eventId)
            if (result.isSuccess) {
                _roleMembers.value = result.getOrThrow()
            }
        }
    }

    fun loadExpenses(eventId: String) {
        viewModelScope.launch {
            val result = repository.getExpenses(eventId)
            _expenses.value = result
        }
    }

    fun loadGroupExpenses(groupId: String) {
        viewModelScope.launch {
            val result = repository.getExpensesByGroup(groupId)
            if (result.isSuccess) {
                _expenses.value = result.getOrDefault(emptyList())
            }
        }
    }

    private val _taskOpState = MutableStateFlow<RoleOpState>(RoleOpState.Idle)
    val taskOpState: StateFlow<RoleOpState> = _taskOpState.asStateFlow()

    fun loadTasks(eventId: String) {
        viewModelScope.launch {
            val result = repository.getTasks(eventId)
            _tasks.value = result
        }
    }

    fun addTask(task: com.tuapp.eventos.domain.model.EventTask) {
        viewModelScope.launch {
            _taskOpState.value = RoleOpState.Loading
            val result = repository.createTask(task)
            if (result.isSuccess) {
                _taskOpState.value = RoleOpState.Success
                loadTasks(task.eventId)
            } else {
                _taskOpState.value = RoleOpState.Error(result.exceptionOrNull()?.message ?: "Error al crear tarea")
            }
        }
    }

    fun updateTask(task: com.tuapp.eventos.domain.model.EventTask) {
        viewModelScope.launch {
            _taskOpState.value = RoleOpState.Loading
            val result = repository.updateTask(task)
            if (result.isSuccess) {
                _taskOpState.value = RoleOpState.Success
                loadTasks(task.eventId)
            } else {
                _taskOpState.value = RoleOpState.Error(result.exceptionOrNull()?.message ?: "Error al actualizar tarea")
            }
        }
    }

    fun deleteTask(taskId: String, eventId: String) {
        viewModelScope.launch {
            _taskOpState.value = RoleOpState.Loading
            val result = repository.deleteTask(taskId)
            if (result.isSuccess) {
                _taskOpState.value = RoleOpState.Success
                loadTasks(eventId)
            } else {
                _taskOpState.value = RoleOpState.Error(result.exceptionOrNull()?.message ?: "Error al eliminar tarea")
            }
        }
    }

    fun notifyTask(task: com.tuapp.eventos.domain.model.EventTask) {
        val userId = SupabaseModule.client.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            _taskOpState.value = RoleOpState.Loading
            val eventName = _event.value?.name ?: "Evento"
            val message = "Nueva notificación para la tarea: ${task.title} (Evento: $eventName)"
            val result = repository.notifyTask(task.id!!, task.eventId, task.roleId, userId, message)
            if (result.isSuccess) {
                _taskOpState.value = RoleOpState.Success
            } else {
                _taskOpState.value = RoleOpState.Error(result.exceptionOrNull()?.message ?: "Error al enviar notificación")
            }
        }
    }

    fun resetTaskOpState() {
        _taskOpState.value = RoleOpState.Idle
    }

    fun addExpense(eventId: String, expense: Expense) {
        viewModelScope.launch {
            _expenseOpState.value = RoleOpState.Loading
            val result = repository.addExpense(eventId, expense)
            if (result.isSuccess) {
                _expenseOpState.value = RoleOpState.Success
                loadExpenses(eventId)
            } else {
                _expenseOpState.value = RoleOpState.Error(result.exceptionOrNull()?.message ?: "Error al añadir gasto")
            }
        }
    }

    fun updateExpense(eventId: String, expense: Expense) {
        viewModelScope.launch {
            _expenseOpState.value = RoleOpState.Loading
            val result = repository.updateExpense(expense)
            if (result.isSuccess) {
                _expenseOpState.value = RoleOpState.Success
                loadExpenses(eventId)
            } else {
                _expenseOpState.value = RoleOpState.Error(result.exceptionOrNull()?.message ?: "Error al actualizar gasto")
            }
        }
    }

    fun deleteExpense(eventId: String, expenseId: String) {
        viewModelScope.launch {
            _expenseOpState.value = RoleOpState.Loading
            val result = repository.deleteExpense(expenseId)
            if (result.isSuccess) {
                _expenseOpState.value = RoleOpState.Success
                loadExpenses(eventId)
            } else {
                _expenseOpState.value = RoleOpState.Error(result.exceptionOrNull()?.message ?: "Error al eliminar gasto")
            }
        }
    }

    fun resetExpenseOpState() {
        _expenseOpState.value = RoleOpState.Idle
    }

    fun toggleRoleAssignment(roleId: String, userId: String, eventId: String, isAssigning: Boolean) {
        viewModelScope.launch {
            _roleOpState.value = RoleOpState.Loading

            if (isAssigning) {
                // Verificar si el rol ya alcanzó el máximo
                val role = _roles.value.find { it.id == roleId }
                val currentAssignedCount = _roleMembers.value.count { it.roleId == roleId }
                
                if (role?.maxPeople != null && currentAssignedCount >= role.maxPeople!!) {
                    _roleOpState.value = RoleOpState.Error("Este rol ya alcanzó el número máximo de personas (${role.maxPeople})")
                    return@launch
                }
            }

            val result = if (isAssigning) {
                repository.assignRoleToUser(roleId, userId, eventId)
            } else {
                repository.removeRoleFromUser(roleId, userId)
            }

            if (result.isSuccess) {
                _roleOpState.value = RoleOpState.Success
                loadRoleMembers(eventId)
            } else {
                _roleOpState.value = RoleOpState.Error(result.exceptionOrNull()?.message ?: "Error al asignar rol")
            }
        }
    }

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            val event = repository.getEventById(eventId)
            _event.value = event
        }
    }

    fun createRole(role: Role) {
        viewModelScope.launch {
            _roleOpState.value = RoleOpState.Loading
            val result = repository.createRole(role)
            if (result.isSuccess) {
                _roleOpState.value = RoleOpState.Success
                role.eventId?.let { loadRoles(it) }
            } else {
                _roleOpState.value = RoleOpState.Error(result.exceptionOrNull()?.message ?: "Error al crear rol")
            }
        }
    }

    fun updateRole(role: Role) {
        viewModelScope.launch {
            _roleOpState.value = RoleOpState.Loading
            val result = repository.updateRole(role)
            if (result.isSuccess) {
                _roleOpState.value = RoleOpState.Success
                role.eventId?.let { loadRoles(it) }
            } else {
                _roleOpState.value = RoleOpState.Error(result.exceptionOrNull()?.message ?: "Error al actualizar rol")
            }
        }
    }

    fun deleteRole(roleId: String, eventId: String) {
        viewModelScope.launch {
            _roleOpState.value = RoleOpState.Loading
            val result = repository.deleteRole(roleId)
            if (result.isSuccess) {
                _roleOpState.value = RoleOpState.Success
                loadRoles(eventId)
            } else {
                _roleOpState.value = RoleOpState.Error(result.exceptionOrNull()?.message ?: "Error al eliminar rol")
            }
        }
    }

    fun startEvent(eventId: String, autoAssign: Boolean = false) {
        viewModelScope.launch {
            _roleOpState.value = RoleOpState.Loading
            
            if (autoAssign) {
                performAutoAssignment(eventId)
            }

            val result = repository.updateEventStatus(eventId, "started")
            if (result.isSuccess) {
                loadEvent(eventId)
                _roleOpState.value = RoleOpState.Success
            } else {
                _roleOpState.value = RoleOpState.Error("Error al iniciar evento")
            }
        }
    }

    private suspend fun performAutoAssignment(eventId: String) {
        val currentRoles = _roles.value
        val currentMembers = _roleMembers.value
        val allParticipants = _participants.value
        
        val newAssignments = mutableListOf<EventRoleMember>()
        
        // 1. Identificar roles que necesitan gente
        for (role in currentRoles) {
            val minNeeded = role.minPeople ?: if (role.isMandatory) 1 else 0
            val maxAllowed = role.maxPeople ?: Int.MAX_VALUE
            val currentAssignedCount = currentMembers.count { it.roleId == role.id }
            
            if (currentAssignedCount < minNeeded) {
                val stillNeeded = minNeeded - currentAssignedCount
                val capacityLeft = maxAllowed - currentAssignedCount
                
                val toAssign = minOf(stillNeeded, capacityLeft)
                
                if (toAssign <= 0) continue

                // 2. Buscar candidatos (priorizando los que no tienen roles obligatorios)
                val candidates = allParticipants.filter { participant ->
                    // No está ya en este rol
                    currentMembers.none { it.roleId == role.id && it.userId == participant.userId }
                }.sortedBy { participant ->
                    // Prioridad: menos roles asignados actualmente
                    currentMembers.count { it.userId == participant.userId }
                }

                candidates.take(toAssign).forEach { candidate ->
                    newAssignments.add(EventRoleMember(
                        roleId = role.id!!,
                        userId = candidate.userId,
                        eventId = eventId
                    ))
                }
            }
        }

        if (newAssignments.isNotEmpty()) {
            repository.assignMultipleRoles(newAssignments)
            loadRoleMembers(eventId)
        }
    }

    fun finishEvent(eventId: String) {
        viewModelScope.launch {
            val result = repository.updateEventStatus(eventId, "finished")
            if (result.isSuccess) {
                loadEvent(eventId)
            }
        }
    }

    fun checkParticipation(eventId: String, userId: String) {
        viewModelScope.launch {
            val result = repository.isUserParticipating(eventId, userId)
            if (result.isSuccess) {
                _isParticipating.value = result.getOrThrow()
            }
        }
    }

    fun createEvent(event: Event, roles: List<Role>) {
        viewModelScope.launch {
            _createEventState.value = CreateEventState.Loading
            val result = repository.createEvent(event, roles)
            if (result.isSuccess) {
                _createEventState.value = CreateEventState.Success
            } else {
                _createEventState.value = CreateEventState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch {
            _createEventState.value = CreateEventState.Loading
            val result = repository.updateEvent(event)
            if (result.isSuccess) {
                _createEventState.value = CreateEventState.Success
                // Actualizar el evento actual si es el mismo que tenemos cargado
                if (_event.value?.id == event.id) {
                    _event.value = event
                }
            } else {
                _createEventState.value = CreateEventState.Error(result.exceptionOrNull()?.message ?: "Error al actualizar")
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            _createEventState.value = CreateEventState.Loading
            val result = repository.deleteEvent(eventId)
            if (result.isSuccess) {
                _createEventState.value = CreateEventState.Success
            } else {
                _createEventState.value = CreateEventState.Error(result.exceptionOrNull()?.message ?: "Error al eliminar")
            }
        }
    }

    fun toggleParticipation(eventId: String, userId: String, isJoining: Boolean, isOwner: Boolean = false) {
        if (!isJoining && isOwner) {
            _joinEventState.value = JoinEventState.Error("El creador no puede abandonar el evento")
            return
        }
        viewModelScope.launch {
            _joinEventState.value = JoinEventState.Loading
            val result = if (isJoining) {
                repository.joinEvent(eventId, userId)
            } else {
                repository.leaveEvent(eventId, userId)
            }
            
            if (result.isSuccess) {
                _joinEventState.value = JoinEventState.Success
                _isParticipating.value = isJoining
                loadParticipants(eventId) // Refresh participants count/list
            } else {
                _joinEventState.value = JoinEventState.Error(result.exceptionOrNull()?.message ?: "Error de participación")
            }
        }
    }

    fun resetCreateState() {
        _createEventState.value = CreateEventState.Idle
    }

    fun resetJoinState() {
        _joinEventState.value = JoinEventState.Idle
    }

    fun resetRoleOpState() {
        _roleOpState.value = RoleOpState.Idle
    }

    sealed class EventsState {
        object Loading : EventsState()
        data class Success(val events: List<Event>) : EventsState()
        data class Error(val message: String) : EventsState()
    }

    sealed class CreateEventState {
        object Idle : CreateEventState()
        object Loading : CreateEventState()
        object Success : CreateEventState()
        data class Error(val message: String) : CreateEventState()
    }

    sealed class JoinEventState {
        object Idle : JoinEventState()
        object Loading : JoinEventState()
        object Success : JoinEventState()
        data class Error(val message: String) : JoinEventState()
    }

    sealed class RoleOpState {
        object Idle : RoleOpState()
        object Loading : RoleOpState()
        object Success : RoleOpState()
        data class Error(val message: String) : RoleOpState()
    }
}
