package com.tuapp.eventos.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.eventos.data.model.GroupMember
import com.tuapp.eventos.data.model.Profile
import com.tuapp.eventos.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MemberViewModel : ViewModel() {

    private val repository = GroupRepository()

    private val _membersState = MutableStateFlow<MembersState>(MembersState.Loading)
    val membersState: StateFlow<MembersState> = _membersState.asStateFlow()

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _inviteState = MutableStateFlow<InviteState>(InviteState.Idle)
    val inviteState: StateFlow<InviteState> = _inviteState.asStateFlow()

    fun loadMembers(groupId: String) {
        viewModelScope.launch {
            _membersState.value = MembersState.Loading
            val result = repository.getGroupMembers(groupId)
            if (result.isSuccess) {
                _membersState.value = MembersState.Success(result.getOrDefault(emptyList()))
            } else {
                _membersState.value = MembersState.Error(result.exceptionOrNull()?.message ?: "Error al cargar miembros")
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchState.value = SearchState.Idle
            return
        }
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            val result = repository.searchUsers(query)
            if (result.isSuccess) {
                _searchState.value = SearchState.Success(result.getOrDefault(emptyList()))
            } else {
                _searchState.value = SearchState.Error(result.exceptionOrNull()?.message ?: "Error en la búsqueda")
            }
        }
    }

    fun inviteUser(groupId: String, receiverId: String, senderId: String) {
        viewModelScope.launch {
            _inviteState.value = InviteState.Loading
            val result = repository.inviteUserToGroup(groupId, receiverId, senderId)
            if (result.isSuccess) {
                _inviteState.value = InviteState.Success
            } else {
                _inviteState.value = InviteState.Error(result.exceptionOrNull()?.message ?: "Error al invitar")
            }
        }
    }

    fun resetInviteState() {
        _inviteState.value = InviteState.Idle
    }

    sealed class MembersState {
        object Loading : MembersState()
        data class Success(val members: List<Pair<GroupMember, Profile>>) : MembersState()
        data class Error(val message: String) : MembersState()
    }

    sealed class SearchState {
        object Idle : SearchState()
        object Loading : SearchState()
        data class Success(val users: List<Profile>) : SearchState()
        data class Error(val message: String) : SearchState()
    }

    sealed class InviteState {
        object Idle : InviteState()
        object Loading : InviteState()
        object Success : InviteState()
        data class Error(val message: String) : InviteState()
    }
}
