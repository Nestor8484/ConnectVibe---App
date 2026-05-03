package com.tuapp.eventos.domain.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class EventTask(
    @SerialName("id")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    
    @SerialName("event_id")
    val eventId: String,

    @SerialName("role_id")
    val roleId: String,
    
    @SerialName("title")
    val title: String,
    
    @SerialName("description")
    val description: String? = null,
    
    @SerialName("status")
    val status: String = "pending", // "pending", "in_progress", "completed"
    
    @SerialName("is_completed")
    val isCompleted: Boolean = false,
    
    @Serializable(with = TimestampSerializer::class)
    @SerialName("created_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val createdAt: Date? = null
)
