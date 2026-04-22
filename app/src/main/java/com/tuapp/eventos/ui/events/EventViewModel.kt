package com.tuapp.eventos.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.eventos.domain.model.Event
import com.tuapp.eventos.domain.usecases.CreateEventUseCase
import com.tuapp.eventos.domain.usecases.GetEventsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventViewModel (
    private val getEventsUseCase: GetEventsUseCase,
    private val createEventUseCase: CreateEventUseCase
) : ViewModel() {

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    fun loadEvents() {
        viewModelScope.launch {
            getEventsUseCase().collect {
                _events.value = it
            }
        }
    }

    fun createEvent(event: Event) {
        viewModelScope.launch {
            createEventUseCase(event)
        }
    }
}
