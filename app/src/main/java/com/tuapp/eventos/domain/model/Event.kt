package com.tuapp.eventos.domain.model

import java.util.Date

data class Event(
    val id: String,
    val title: String,
    val description: String,
    val date: Date,
    val location: String,
    val isPublic: Boolean,
    val ownerId: String,
    val roles: List<Role> = emptyList(),
    val participants: List<Participant> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val tasks: List<EventTask> = emptyList(),
    val settings: EventSettings = EventSettings()
)

data class EventSettings(
    val customRolesEnabled: Boolean = true,
    val finesEnabled: Boolean = false,
    val notificationReminderMinutes: Int = 30
)

data class Participant(
    val userId: String,
    val userName: String,
    val roleId: String? = null
)
