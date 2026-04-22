package com.tuapp.eventos.domain.model

data class Role(
    val id: String,
    val name: String,
    val description: String,
    val permissions: List<String> = emptyList(),
    val fineAmount: Double = 0.0
)
