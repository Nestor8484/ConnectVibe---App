package com.tuapp.eventos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val full_name: String? = null,
    val username: String? = null,
    val email: String? = null,
    val avatar_url: String? = null,
    val updated_at: String? = null
)
