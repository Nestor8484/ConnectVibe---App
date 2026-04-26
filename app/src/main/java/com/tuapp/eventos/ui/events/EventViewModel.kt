package com.tuapp.eventos.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.eventos.data.repository.EventRepository
import com.tuapp.eventos.data.repository.EventRepositoryImpl
import com.tuapp.eventos.domain.model.Event
import com.tuapp.eventos.domain.model.Role
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

    fun loadPublicEvents() {
        viewModelScope.launch {
            _eventsState.value = EventsState.Loading
            repository.getEvents().collect { allEvents ->
                _eventsState.value = EventsState.Success(allEvents.filter { it.visibility == "public" })
            }
        }
    }

    fun loadJoinedEvents(userId: String) {
        viewModelScope.launch {
            _eventsState.value = EventsState.Loading
            repository.getEvents().collect { allEvents ->
                _eventsState.value = EventsState.Success(allEvents.filter { it.createdBy == userId })
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

    fun joinEvent(eventId: String, userId: String) {
        viewModelScope.launch {
            _joinEventState.value = JoinEventState.Loading
            val result = repository.joinEvent(eventId, userId)
            if (result.isSuccess) {
                _joinEventState.value = JoinEventState.Success
            } else {
                _joinEventState.value = JoinEventState.Error(result.exceptionOrNull()?.message ?: "Error al unirse")
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
