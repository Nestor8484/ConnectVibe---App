package com.tuapp.eventos.domain.model

import java.util.Date

data class EventTask(
    val id: String,
    val title: String,
    val description: String,
    val assignedToUserId: String?,
    val deadline: Date?,
    val isCompleted: Boolean = false,
    val reminderSent: Boolean = false
)
