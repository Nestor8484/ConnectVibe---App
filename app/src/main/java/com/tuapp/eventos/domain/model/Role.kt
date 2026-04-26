package com.tuapp.eventos.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class Role(
    @SerialName("id")
    val id: String? = null,
    @SerialName("event_id")
    val eventId: String? = null,
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String? = null,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("created_at")
    val createdAt: Date? = null
)
