package com.tuapp.eventos.domain.usecases

import com.tuapp.eventos.data.repository.EventRepository
import com.tuapp.eventos.domain.model.Debt
import com.tuapp.eventos.domain.model.Expense

class ManageExpensesUseCase (
    private val repository: EventRepository
) {
    suspend fun addExpense(eventId: String, expense: Expense) {
        repository.addExpense(eventId, expense)
    }

    suspend fun calculateSimplifiedDebts(eventId: String): List<Debt> {
        val expenses = repository.getExpenses(eventId)
        return emptyList()
    }
}
