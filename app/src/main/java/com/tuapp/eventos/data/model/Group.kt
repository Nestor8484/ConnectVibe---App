package com.tuapp.eventos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: String? = null,
    val name: String,
    val description: String? = null,
    val created_by: String,
    val created_at: String? = null,
    val icon: String? = "ic_groups",
    val color: String? = "#1565C0"
)
