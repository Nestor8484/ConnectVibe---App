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

    sealed class GroupsState {
        object Loading : GroupsState()
        data class Success(val groups: List<Group>) : GroupsState()
        data class Error(val message: String) : GroupsState()
    }
}
