package com.tuapp.eventos.data.repository

import com.tuapp.eventos.data.model.Group
import com.tuapp.eventos.data.model.GroupMember
import com.tuapp.eventos.di.SupabaseModule
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GroupRepository {
    private val client = SupabaseModule.client

    suspend fun createGroup(name: String, userId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Insert group
                val group = Group(
                    name = name,
                    created_by = userId
                )
                val insertedGroup = client.from("groups").insert(group) {
                    select()
                }.decodeSingle<Group>()

                val groupId = insertedGroup.id ?: throw Exception("Failed to get group ID")

                // 2. Add creator as admin member
                val member = GroupMember(
                    group_id = groupId,
                    user_id = userId,
                    is_admin = true
                )
                client.from("group_members").insert(member)

                Result.success(groupId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun isUserAdmin(groupId: String, userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val member = client.from("group_members")
                    .select {
                        filter {
                            eq("group_id", groupId)
                            eq("user_id", userId)
                            eq("is_admin", true)
                        }
                    }.decodeSingleOrNull<GroupMember>()
                member != null
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun getGroupsForUser(userId: String): Result<List<Group>> {
        return withContext(Dispatchers.IO) {
            try {
                val groups = client.from("groups")
                    .select(Columns.raw("*, group_members!inner(user_id)")) {
                        filter {
                            eq("group_members.user_id", userId)
                        }
                    }.decodeList<Group>()
                Result.success(groups)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
