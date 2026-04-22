package com.tuapp.eventos.domain.usecases

import com.tuapp.eventos.data.repository.EventRepository
import com.tuapp.eventos.domain.model.Event

class CreateEventUseCase (
    private val repository: EventRepository
) {
    suspend operator fun invoke(event: Event) {
        if (event.title.isBlank()) {
            throw IllegalArgumentException("Title cannot be empty")
        }
        repository.createEvent(event)
    }
}
