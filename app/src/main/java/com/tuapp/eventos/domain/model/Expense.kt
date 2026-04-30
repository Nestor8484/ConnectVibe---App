package com.tuapp.eventos.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    @SerialName("id")
    val id: String? = null,
    @SerialName("event_id")
    val eventId: String,
    @SerialName("description")
    val description: String,
    @SerialName("amount")
    val amount: Double,
    @SerialName("payer_id")
    val payerId: String,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("date")
    val date: java.util.Date,
    @SerialName("receipt_url")
    val receiptUrl: String? = null,
    @SerialName("split_strategy")
    val splitStrategy: SplitStrategy = SplitStrategy.EQUALLY
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
