package com.tuapp.eventos.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.eventos.data.repository.EventRepository
import com.tuapp.eventos.data.repository.EventRepositoryImpl
import com.tuapp.eventos.domain.model.Event
import com.tuapp.eventos.domain.model.Role
import com.tuapp.eventos.domain.model.GroupMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventViewModel : ViewModel() {

    private val repository: EventRepository = EventRepositoryImpl()

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
            repository.getEventsByGroup(groupId).collect { events ->
                _eventsState.value = EventsState.Success(events)
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

    fun toggleParticipation(eventId: String, userId: String, isJoining: Boolean) {
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
}
