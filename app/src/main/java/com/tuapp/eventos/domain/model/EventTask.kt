package com.tuapp.eventos.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class EventTask(
    @SerialName("id")
    val id: String? = null,
    
    @SerialName("event_id")
    val eventId: String,
    
    @SerialName("title")
    val title: String,
    
    @SerialName("description")
    val description: String? = null,
    
    @SerialName("assigned_to_user_id")
    val assignedToUserId: String? = null,
    
    @Serializable(with = TimestampSerializer::class)
    @SerialName("deadline")
    val deadline: Date? = null,
    
    @SerialName("is_completed")
    val isCompleted: Boolean = false,
    
    @SerialName("reminder_sent")
    val reminderSent: Boolean = false
)
