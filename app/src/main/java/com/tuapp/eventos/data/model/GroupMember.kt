package com.tuapp.eventos.data.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class GroupMember(
    val group_id: String,
    val user_id: String,
    val status: String = "active",
    val is_admin: Boolean = false,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val created_at: String? = null
)
