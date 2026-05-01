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
    
    @SerialName("name")
    val name: String,
    
    @SerialName("amount")
    val amount: Double,
    
    @SerialName("category")
    val category: String,
    
    @SerialName("payer_id")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val payerId: String? = null,
    
    @Serializable(with = TimestampSerializer::class)
    @SerialName("date")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val date: java.util.Date? = null,

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
