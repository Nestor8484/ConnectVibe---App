package com.tuapp.eventos.data.repository

import com.tuapp.eventos.domain.model.Event
import com.tuapp.eventos.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getEvents(): Flow<List<Event>>
    suspend fun getEventById(id: String): Event?
    suspend fun createEvent(event: Event)
    suspend fun deleteEvent(event: Event)
    
    // Expenses
    suspend fun addExpense(eventId: String, expense: Expense)
    suspend fun getExpenses(eventId: String): List<Expense>
}
