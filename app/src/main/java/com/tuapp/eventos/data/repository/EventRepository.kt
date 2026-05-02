package com.tuapp.eventos.data.repository

import com.tuapp.eventos.domain.model.Event
import com.tuapp.eventos.domain.model.EventRoleMember
import com.tuapp.eventos.domain.model.Expense
import com.tuapp.eventos.domain.model.Role
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getEvents(): Flow<List<Event>>
    suspend fun getEventById(id: String): Event?
    suspend fun createEvent(event: Event, roles: List<Role>): Result<String>
    suspend fun updateEvent(event: Event): Result<Unit>
    suspend fun joinEvent(eventId: String, userId: String): Result<Unit>
    suspend fun leaveEvent(eventId: String, userId: String): Result<Unit>
    suspend fun getParticipatingEventIds(userId: String): Result<List<String>>
    suspend fun isUserParticipating(eventId: String, userId: String): Result<Boolean>
    suspend fun getEventParticipants(eventId: String): Result<List<com.tuapp.eventos.domain.model.GroupMember>>
    suspend fun updateParticipantRole(eventId: String, userId: String, isAdmin: Boolean): Result<Unit>
    suspend fun deleteEvent(eventId: String): Result<Unit>
    
    // Roles
    suspend fun getRoles(eventId: String): Result<List<Role>>
    suspend fun createRole(role: Role): Result<Unit>
    suspend fun updateRole(role: Role): Result<Unit>
    suspend fun deleteRole(roleId: String): Result<Unit>
    suspend fun assignRoleToUser(roleId: String, userId: String, eventId: String): Result<Unit>
    suspend fun removeRoleFromUser(roleId: String, userId: String): Result<Unit>
    suspend fun getRoleMembers(eventId: String): Result<List<EventRoleMember>>
    suspend fun updateEventStatus(eventId: String, status: String): Result<Unit>
    suspend fun assignMultipleRoles(assignments: List<EventRoleMember>): Result<Unit>

    suspend fun addExpense(eventId: String, expense: Expense): Result<Unit>
    suspend fun updateExpense(expense: Expense): Result<Unit>
    suspend fun deleteExpense(expenseId: String): Result<Unit>
    suspend fun getExpenses(eventId: String): List<Expense>
    
    // Tasks
    suspend fun getTasks(eventId: String): List<com.tuapp.eventos.domain.model.EventTask>
    suspend fun createTask(task: com.tuapp.eventos.domain.model.EventTask): Result<Unit>
    suspend fun updateTask(task: com.tuapp.eventos.domain.model.EventTask): Result<Unit>
    suspend fun deleteTask(taskId: String): Result<Unit>
    suspend fun notifyTask(taskId: String, eventId: String, roleId: String, senderId: String, message: String): Result<Unit>

    fun getEventsByGroup(groupId: String): Flow<List<Event>>
    suspend fun getExpensesByGroup(groupId: String): Result<List<Expense>>
}
