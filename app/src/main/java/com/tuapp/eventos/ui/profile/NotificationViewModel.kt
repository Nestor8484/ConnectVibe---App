package com.tuapp.eventos.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.eventos.data.model.Group
import com.tuapp.eventos.data.model.Notification
import com.tuapp.eventos.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {

    private val repository = GroupRepository()

    private val _notificationsState = MutableStateFlow<NotificationsState>(NotificationsState.Loading)
    val notificationsState: StateFlow<NotificationsState> = _notificationsState.asStateFlow()

    fun loadNotifications(userId: String) {
        viewModelScope.launch {
            _notificationsState.value = NotificationsState.Loading
            val result = repository.getNotifications(userId)
            if (result.isSuccess) {
                _notificationsState.value = NotificationsState.Success(result.getOrDefault(emptyList()))
            } else {
                _notificationsState.value = NotificationsState.Error(result.exceptionOrNull()?.message ?: "Error al cargar notificaciones")
            }
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

    sealed class NotificationsState {
        object Loading : NotificationsState()
        data class Success(val notifications: List<Pair<Notification, Group>>) : NotificationsState()
        data class Error(val message: String) : NotificationsState()
    }
}
