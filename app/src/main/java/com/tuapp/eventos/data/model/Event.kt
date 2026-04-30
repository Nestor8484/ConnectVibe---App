package com.tuapp.eventos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: String? = null,
    val created_by: String,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val visibility: String = "private", // "private" or "public"
    val group_id: String? = null,
    val start_date: String? = null,
    val end_date: String? = null,
    val settings: String? = null, // JSON string or object
    val icon: String? = null,
    val color: String? = null
)
