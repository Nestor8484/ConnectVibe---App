package com.tuapp.eventos.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.eventos.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState

    fun register(email: String, pass: String, fullName: String, username: String) {
        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            val result = authRepository.signUp(email, pass, fullName, username)
            if (result.isSuccess) {
                _registrationState.value = RegistrationState.Success
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Error desconocido"
                android.util.Log.e("RegisterViewModel", "Error en registro: $errorMsg")
                _registrationState.value = RegistrationState.Error(errorMsg)
            }
        }
    }

    sealed class RegistrationState {
        object Idle : RegistrationState()
        object Loading : RegistrationState()
        object Success : RegistrationState()
        data class Error(val message: String) : RegistrationState()
    }
}
