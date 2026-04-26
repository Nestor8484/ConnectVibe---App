package com.tuapp.eventos.data.repository

import com.tuapp.eventos.domain.model.Event
import com.tuapp.eventos.domain.model.Expense
import com.tuapp.eventos.domain.model.Role
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getEvents(): Flow<List<Event>>
    suspend fun getEventById(id: String): Event?
    suspend fun createEvent(event: Event, roles: List<Role>): Result<String>
    suspend fun joinEvent(eventId: String, userId: String): Result<Unit>
    suspend fun leaveEvent(eventId: String, userId: String): Result<Unit>
    suspend fun getParticipatingEventIds(userId: String): Result<List<String>>
    suspend fun isUserParticipating(eventId: String, userId: String): Result<Boolean>
    suspend fun getEventParticipants(eventId: String): Result<List<com.tuapp.eventos.domain.model.GroupMember>>
    suspend fun deleteEvent(event: Event)
    suspend fun addExpense(eventId: String, expense: Expense)
    suspend fun getExpenses(eventId: String): List<Expense>
    fun getEventsByGroup(groupId: String): Flow<List<Event>>
}
