package com.tuapp.eventos.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class EventMember(
    @SerialName("event_id")
    val eventId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("status")
    val status: String = "active", // Match DB default 'active'
    @Serializable(with = TimestampSerializer::class)
    @SerialName("joined_at")
    val joinedAt: Date? = null,
    @SerialName("role_id")
    val roleId: String? = null,
    @SerialName("is_admin")
    val isAdmin: Boolean = false,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("created_at")
    val createdAt: Date? = null
)
