package com.tuapp.eventos.domain.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Expense(
    @SerialName("id")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    
    @SerialName("event_id")
    val eventId: String,

    @SerialName("created_by")
    val createdBy: String,
    
    @SerialName("title")
    val title: String,

    @SerialName("category")
    val category: String,

    @SerialName("description")
    val description: String? = null,
    
    @SerialName("amount")
    val amount: Double,

    @SerialName("currency")
    val currency: String = "EUR",
    
    @SerialName("paid_by_user_id")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val paidByUserId: String? = null,
    
    @Serializable(with = TimestampSerializer::class)
    @SerialName("incurred_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val incurredAt: java.util.Date? = null,

    @Serializable(with = TimestampSerializer::class)
    @SerialName("created_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val createdAt: java.util.Date? = null
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
