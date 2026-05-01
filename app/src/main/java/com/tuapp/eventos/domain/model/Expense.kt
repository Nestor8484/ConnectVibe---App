package com.tuapp.eventos.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    @SerialName("id")
    val id: String? = null,
    @SerialName("event_id")
    val eventId: String,
    @SerialName("name")
    val name: String,
    @SerialName("amount")
    val amount: Double,
    @SerialName("category")
    val category: String,
    @SerialName("payer_id")
    val payerId: String? = null,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("date")
    val date: java.util.Date? = null
)

@Serializable
enum class SplitStrategy {
    EQUALLY,
    BY_PERCENTAGE,
    BY_AMOUNT
}

data class Debt(
    val debtorId: String,
    val creditorId: String,
    val amount: Double
)
