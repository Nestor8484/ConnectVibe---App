package com.tuapp.eventos.domain.model

data class Expense(
    val id: String,
    val description: String,
    val amount: Double,
    val payerId: String,
    val date: java.util.Date,
    val receiptUrl: String? = null,
    val splitStrategy: SplitStrategy = SplitStrategy.EQUALLY
)

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
