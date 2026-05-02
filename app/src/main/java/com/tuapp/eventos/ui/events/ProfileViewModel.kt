package com.tuapp.eventos.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.eventos.data.model.Profile
import com.tuapp.eventos.data.repository.AuthRepository
import com.tuapp.eventos.data.repository.GroupRepository
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.auth.auth
import com.tuapp.eventos.di.SupabaseModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val groupRepository = GroupRepository()

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState

    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState

    private val _pendingNotificationsCount = MutableStateFlow(0)
    val pendingNotificationsCount: StateFlow<Int> = _pendingNotificationsCount

    init {
        fetchProfile()
        setupNotificationRealtime()
    }

    fun fetchProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            val result = authRepository.getCurrentProfile()
            val email = authRepository.getCurrentUserEmail() ?: ""
            
            if (result.isSuccess) {
                val profile = result.getOrThrow()
                _profileState.value = ProfileState.Success(profile, email)
                fetchPendingNotificationsCount(profile.id)
            } else {
                _profileState.value = ProfileState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    private fun fetchPendingNotificationsCount(userId: String) {
        viewModelScope.launch {
            val result = groupRepository.getNotifications(userId)
            if (result.isSuccess) {
                val notifications = result.getOrThrow()
                _pendingNotificationsCount.value = notifications.count { it.status == "pending" }
            }
        }
    }

    private fun setupNotificationRealtime() {
        val userId = SupabaseModule.client.auth.currentUserOrNull()?.id ?: return
        val channel = SupabaseModule.client.realtime.channel("profile-notifications")
        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "notifications"
        }.onEach {
            fetchPendingNotificationsCount(userId)
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            channel.subscribe()
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
