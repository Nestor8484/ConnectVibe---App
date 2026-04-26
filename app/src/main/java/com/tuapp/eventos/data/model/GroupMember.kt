package com.tuapp.eventos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GroupMember(
    val group_id: String,
    val user_id: String,
    val is_admin: Boolean = false,
    val joined_at: String? = null
)
