package com.tuapp.eventos.domain.usecases

import com.tuapp.eventos.data.repository.EventRepository
import com.tuapp.eventos.domain.model.Event
import com.tuapp.eventos.domain.model.Role

class CreateEventUseCase (
    private val repository: EventRepository
) {
    suspend operator fun invoke(event: Event, roles: List<Role> = emptyList()) {
        repository.createEvent(event, roles)
    }
}
