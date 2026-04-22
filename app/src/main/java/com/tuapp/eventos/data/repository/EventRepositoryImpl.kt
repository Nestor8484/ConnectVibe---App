package com.tuapp.eventos.data.repository

import com.tuapp.eventos.data.local.dao.EventDao
import com.tuapp.eventos.domain.model.Event
import com.tuapp.eventos.domain.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class EventRepositoryImpl (
    private val eventDao: EventDao
) : EventRepository {
    override fun getEvents(): Flow<List<Event>> {
        return flowOf(emptyList())
    }

    override suspend fun getEventById(id: String): Event? {
        return null
    }

    override suspend fun createEvent(event: Event) {
    }

    override suspend fun deleteEvent(event: Event) {
    }

    override suspend fun addExpense(eventId: String, expense: Expense) {
    }

    override suspend fun getExpenses(eventId: String): List<Expense> {
        return emptyList()
    }
}
