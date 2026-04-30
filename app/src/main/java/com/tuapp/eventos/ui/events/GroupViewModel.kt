package com.tuapp.eventos.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.eventos.data.model.Group
import com.tuapp.eventos.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupViewModel : ViewModel() {

    private val repository = GroupRepository()

    private val _groupsState = MutableStateFlow<GroupsState>(GroupsState.Loading)
    val groupsState: StateFlow<GroupsState> = _groupsState.asStateFlow()

    private val _currentGroup = MutableStateFlow<Group?>(null)
    val currentGroup: StateFlow<Group?> = _currentGroup.asStateFlow()

    fun loadGroups(userId: String) {
        viewModelScope.launch {
            _groupsState.value = GroupsState.Loading
            val result = repository.getGroupsForUser(userId)
            if (result.isSuccess) {
                _groupsState.value = GroupsState.Success(result.getOrDefault(emptyList()))
            } else {
                _groupsState.value = GroupsState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    fun loadGroup(groupId: String) {
        viewModelScope.launch {
            val result = repository.getGroupById(groupId)
            if (result.isSuccess) {
                _currentGroup.value = result.getOrNull()
            }
        }
    }

    private val _updateState = MutableStateFlow<Result<Unit>?>(null)
    val updateState: StateFlow<Result<Unit>?> = _updateState.asStateFlow()

    fun updateGroup(groupId: String, name: String, description: String?, icon: String, color: String) {
        viewModelScope.launch {
            val result = repository.updateGroup(groupId, name, description, icon, color)
            _updateState.value = result
            if (result.isSuccess) {
                loadGroup(groupId) // Recargar datos locales
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = null
    }

    sealed class GroupsState {
        object Loading : GroupsState()
        data class Success(val groups: List<Group>) : GroupsState()
        data class Error(val message: String) : GroupsState()
    }
}
