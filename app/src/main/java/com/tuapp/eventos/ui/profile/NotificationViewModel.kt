package com.tuapp.eventos.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.eventos.data.model.Group
import com.tuapp.eventos.data.model.Notification
import com.tuapp.eventos.data.repository.GroupRepository
import com.tuapp.eventos.di.SupabaseModule
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {

    private val repository = GroupRepository()

    private val _notificationsState = MutableStateFlow<NotificationsState>(NotificationsState.Loading)
    val notificationsState: StateFlow<NotificationsState> = _notificationsState.asStateFlow()

    private val _hasPendingNotifications = MutableStateFlow(false)
    val hasPendingNotifications: StateFlow<Boolean> = _hasPendingNotifications.asStateFlow()

    fun loadNotifications(userId: String) {
        viewModelScope.launch {
            _notificationsState.value = NotificationsState.Loading
            val result = repository.getNotifications(userId)
            if (result.isSuccess) {
                val notifications = result.getOrDefault(emptyList())
                _notificationsState.value = NotificationsState.Success(notifications)
                _hasPendingNotifications.value = notifications.any { it.first.status == "pending" }
            } else {
                _notificationsState.value = NotificationsState.Error(result.exceptionOrNull()?.message ?: "Error al cargar notificaciones")
            }
        }
        setupRealtime(userId)
    }

    private fun setupRealtime(userId: String) {
        val channel = SupabaseModule.client.realtime.channel("notifications-changes")
        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "notifications"
        }.onEach { action ->
            when (action) {
                is PostgresAction.Insert, is PostgresAction.Update, is PostgresAction.Delete -> {
                    val result = repository.getNotifications(userId)
                    if (result.isSuccess) {
                        val notifications = result.getOrDefault(emptyList())
                        _notificationsState.value = NotificationsState.Success(notifications)
                        _hasPendingNotifications.value = notifications.any { it.first.status == "pending" }
                    }
                }
                else -> {}
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            channel.subscribe()
        }
    }

    fun acceptInvitation(notification: Notification, userId: String) {
        viewModelScope.launch {
            val result = repository.acceptInvitation(notification)
            if (result.isSuccess) {
                loadNotifications(userId)
            }
        }
    }

    fun declineInvitation(notificationId: String, userId: String) {
        viewModelScope.launch {
            val result = repository.declineInvitation(notificationId)
            if (result.isSuccess) {
                loadNotifications(userId)
            }
        }
    }

    fun deleteNotification(notificationId: String, userId: String) {
        viewModelScope.launch {
            val result = repository.deleteNotification(notificationId)
            if (result.isSuccess) {
                loadNotifications(userId)
            }
        }
    }

    sealed class NotificationsState {
        object Loading : NotificationsState()
        data class Success(val notifications: List<Pair<Notification, Group>>) : NotificationsState()
        data class Error(val message: String) : NotificationsState()
    }
}
