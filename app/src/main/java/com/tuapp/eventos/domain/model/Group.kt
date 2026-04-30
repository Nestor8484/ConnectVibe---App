package com.tuapp.eventos.domain.model

data class Group(
    val id: String,
    val name: String,
    val description: String?,
    val ownerId: String,
    val members: List<GroupMember> = emptyList(),
    val events: List<Event> = emptyList(),
    val icon: String? = "ic_groups",
    val color: String? = "#1565C0"
)

data class GroupMember(
    val userId: String,
    val userName: String,
    val role: MemberRole = MemberRole.MEMBER,
    val email: String = ""
)

enum class MemberRole {
    ADMIN,
    MEMBER
}
