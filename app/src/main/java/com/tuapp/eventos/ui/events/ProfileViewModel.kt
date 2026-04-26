package com.tuapp.eventos.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.eventos.data.model.Profile
import com.tuapp.eventos.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState

    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState

    init {
        fetchProfile()
    }

    fun fetchProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            val result = authRepository.getCurrentProfile()
            val email = authRepository.getCurrentUserEmail() ?: ""
            
            if (result.isSuccess) {
                _profileState.value = ProfileState.Success(result.getOrThrow(), email)
            } else {
                _profileState.value = ProfileState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _logoutState.value = LogoutState.Loading
            try {
                authRepository.signOut()
                _logoutState.value = LogoutState.Success
            } catch (e: Exception) {
                _logoutState.value = LogoutState.Error(e.message ?: "Error al cerrar sesión")
            }
        }
    }

    sealed class ProfileState {
        object Loading : ProfileState()
        data class Success(val profile: Profile, val email: String) : ProfileState()
        data class Error(val message: String) : ProfileState()
    }

    sealed class LogoutState {
        object Idle : LogoutState()
        object Loading : LogoutState()
        object Success : LogoutState()
        data class Error(val message: String) : LogoutState()
    }
}
