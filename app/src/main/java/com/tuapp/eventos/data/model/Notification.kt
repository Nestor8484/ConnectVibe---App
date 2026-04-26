package com.tuapp.eventos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String? = null,
    val receiver_id: String,
    val sender_id: String,
    val group_id: String,
    val type: String = "group_invitation",
    val message: String? = null,
    val status: String = "pending",
    val created_at: String? = null
)
