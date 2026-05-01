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

    private var isRealtimeSetup = false

    fun loadNotifications(userId: String) {
        viewModelScope.launch {
            _notificationsState.value = NotificationsState.Loading
            refreshNotifications(userId)
        }
        if (!isRealtimeSetup) {
            setupRealtime(userId)
            isRealtimeSetup = true
        }
    }

    private suspend fun refreshNotifications(userId: String) {
        val result = repository.getNotifications(userId)
        if (result.isSuccess) {
            val notifications = result.getOrDefault(emptyList())
            _notificationsState.value = NotificationsState.Success(notifications)
            _hasPendingNotifications.value = notifications.any { it.first.status == "pending" }
        } else {
            _notificationsState.value = NotificationsState.Error(result.exceptionOrNull()?.message ?: "Error al cargar notificaciones")
        }
    }

    private fun setupRealtime(userId: String) {
        val channel = SupabaseModule.client.realtime.channel("notifications-changes")
        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "notifications"
        }.onEach { action ->
            android.util.Log.d("NotificationViewModel", "Realtime action detected: $action")
            refreshNotifications(userId)
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            try {
                channel.subscribe()
            } catch (e: Exception) {
                android.util.Log.e("NotificationViewModel", "Error subscribing to realtime: ${e.message}")
            }
        }
    }

    fun acceptInvitation(notification: Notification, userId: String) {
        viewModelScope.launch {
            val result = repository.acceptInvitation(notification)
            if (result.isSuccess) {
                // El refresco vendrá por Realtime o podemos forzarlo
                refreshNotifications(userId)
            }
        }
    }

    fun declineInvitation(notificationId: String, userId: String) {
        viewModelScope.launch {
            val result = repository.declineInvitation(notificationId)
            if (result.isSuccess) {
                refreshNotifications(userId)
            }
        }
    }

    fun deleteNotification(notificationId: String, userId: String) {
        viewModelScope.launch {
            val result = repository.deleteNotification(notificationId)
            if (result.isSuccess) {
                refreshNotifications(userId)
            }
        }
    }

    sealed class NotificationsState {
        object Loading : NotificationsState()
        data class Success(val notifications: List<Pair<Notification, Group>>) : NotificationsState()
        data class Error(val message: String) : NotificationsState()
    }
}
