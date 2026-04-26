package com.tuapp.eventos.data.repository

import com.tuapp.eventos.di.SupabaseModule
import com.tuapp.eventos.domain.model.Event
import com.tuapp.eventos.domain.model.EventMember
import com.tuapp.eventos.domain.model.Expense
import com.tuapp.eventos.domain.model.Role
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class EventRepositoryImpl : EventRepository {
    
    private val client = SupabaseModule.client

    override fun getEvents(): Flow<List<Event>> = flow {
        try {
            val events = client.from("events")
                .select()
                .decodeList<Event>()
            emit(events)
        } catch (e: Exception) {
            android.util.Log.e("EventRepository", "Error fetching events: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun getEventById(id: String): Event? {
        return try {
            client.from("events")
                .select {
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingle<Event>()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun createEvent(event: Event, roles: List<Role>): Result<String> {
        return try {
            // 1. Create the event using the data class directly to avoid "class Any" serialization issues
            // We use the data class but some fields like id, created_at, updated_at will be null 
            // and handled by Supabase if we configure the insert properly.
            
            val insertedEvent = client.from("events")
                .insert(event) {
                    select()
                }
                .decodeSingle<Event>()
            
            val eventId = insertedEvent.id ?: throw Exception("Failed to get event ID")

            // 2. Insert roles
            if (roles.isNotEmpty()) {
                val rolesToInsert = roles.map { it.copy(eventId = eventId) }
                client.from("event_roles").insert(rolesToInsert)
            }

            // 3. Add owner as admin member
            val ownerMember = EventMember(
                eventId = eventId,
                userId = event.createdBy,
                isAdmin = true,
                status = "joined"
            )
            client.from("event_members").insert(ownerMember)

            Result.success(eventId)
        } catch (e: Exception) {
            android.util.Log.e("EventRepository", "Error creating event: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun joinEvent(eventId: String, userId: String): Result<Unit> {
        return try {
            val member = EventMember(
                eventId = eventId,
                userId = userId,
                isAdmin = false,
                status = "joined"
            )
            client.from("event_members").insert(member)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("EventRepository", "Error joining event: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun deleteEvent(event: Event) {
        try {
            event.id?.let { id ->
                client.from("events").delete {
                    filter {
                        eq("id", id)
                    }
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun addExpense(eventId: String, expense: Expense) {
    }

    override suspend fun getExpenses(eventId: String): List<Expense> {
        return emptyList()
    }
}
