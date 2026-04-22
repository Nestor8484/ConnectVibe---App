package com.tuapp.eventos.domain.usecases

import com.tuapp.eventos.data.repository.EventRepository
import com.tuapp.eventos.domain.model.Event
import kotlinx.coroutines.flow.Flow

class GetEventsUseCase (
    private val repository: EventRepository
) {
    operator fun invoke(): Flow<List<Event>> {
        return repository.getEvents()
    }
}
