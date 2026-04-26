package com.tuapp.eventos.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.eventos.data.model.Profile
import com.tuapp.eventos.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AccountSettingsViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow<AccountSettingsState>(AccountSettingsState.Idle)
    val uiState: StateFlow<AccountSettingsState> = _uiState

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile

    init {
        fetchProfile()
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            val result = authRepository.getCurrentProfile()
            if (result.isSuccess) {
                _currentProfile.value = result.getOrThrow()
            }
        }
    }

    fun updateProfile(fullName: String, username: String) {
        viewModelScope.launch {
            _uiState.value = AccountSettingsState.Loading
            val result = authRepository.updateProfile(fullName, username)
            if (result.isSuccess) {
                _uiState.value = AccountSettingsState.ProfileUpdateSuccess
                fetchProfile()
            } else {
                _uiState.value = AccountSettingsState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            _uiState.value = AccountSettingsState.Loading
            val result = authRepository.updatePassword(newPassword)
            if (result.isSuccess) {
                _uiState.value = AccountSettingsState.PasswordUpdateSuccess
            } else {
                _uiState.value = AccountSettingsState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    fun resetState() {
        _uiState.value = AccountSettingsState.Idle
    }

    sealed class AccountSettingsState {
        object Idle : AccountSettingsState()
        object Loading : AccountSettingsState()
        object ProfileUpdateSuccess : AccountSettingsState()
        object PasswordUpdateSuccess : AccountSettingsState()
        data class Error(val message: String) : AccountSettingsState()
    }
}
