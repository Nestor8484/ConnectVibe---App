package com.tuapp.eventos.domain.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class EventRoleMember(
    @SerialName("id")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("role_id")
    val roleId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("event_id")
    val eventId: String,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("created_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val createdAt: Date? = null
)
