package com.tuapp.eventos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val username: String? = null,
    val full_name: String? = null,
    val avatar_url: String? = null
)
