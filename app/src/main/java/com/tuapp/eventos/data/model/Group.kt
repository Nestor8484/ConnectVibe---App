package com.tuapp.eventos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: String? = null,
    val name: String,
    val created_by: String,
    val created_at: String? = null
)
