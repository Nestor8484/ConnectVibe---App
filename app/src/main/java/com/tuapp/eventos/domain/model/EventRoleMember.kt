package com.tuapp.eventos.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class EventRoleMember(
    @SerialName("id")
    val id: String? = null,
    @SerialName("role_id")
    val roleId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("event_id")
    val eventId: String,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("created_at")
    val createdAt: Date? = null
)
