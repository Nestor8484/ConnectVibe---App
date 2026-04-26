package com.tuapp.eventos.data.repository

import com.tuapp.eventos.data.model.Group
import com.tuapp.eventos.data.model.GroupMember
import com.tuapp.eventos.data.model.Notification
import com.tuapp.eventos.data.model.Profile
import com.tuapp.eventos.di.SupabaseModule
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

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
                    status = "active",
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

    suspend fun getGroupMembers(groupId: String): Result<List<Pair<GroupMember, Profile>>> {
        return withContext(Dispatchers.IO) {
            try {
                // Fetch members
                val members = client.from("group_members")
                    .select {
                        filter {
                            eq("group_id", groupId)
                        }
                    }.decodeList<GroupMember>()
                
                if (members.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                // Fetch profiles for those members manually since there is no direct FK to profiles in group_members
                val userIds = members.map { it.user_id }
                val profiles = client.from("profiles")
                    .select {
                        filter {
                            isIn("id", userIds)
                        }
                    }.decodeList<Profile>()
                
                val profilesMap = profiles.associateBy { it.id }
                
                val resultList = members.mapNotNull { member ->
                    val profile = profilesMap[member.user_id]
                    if (profile != null) {
                        member to profile
                    } else {
                        // Create a fallback profile if not found
                        member to Profile(id = member.user_id, username = "Usuario")
                    }
                }
                
                Result.success(resultList)
            } catch (e: Exception) {
                android.util.Log.e("GroupRepository", "Error fetching members: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun searchUsers(query: String): Result<List<Profile>> {
        return withContext(Dispatchers.IO) {
            try {
                val profiles = client.from("profiles")
                    .select {
                        filter {
                            or {
                                ilike("username", "%$query%")
                                ilike("full_name", "%$query%")
                            }
                        }
                    }.decodeList<Profile>()
                Result.success(profiles)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun sendGroupInvitation(groupId: String, receiverId: String, senderId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Primero verificamos si ya es miembro
                val isMember = client.from("group_members")
                    .select {
                        filter {
                            eq("group_id", groupId)
                            eq("user_id", receiverId)
                        }
                    }.decodeSingleOrNull<GroupMember>() != null

                if (isMember) {
                    return@withContext Result.failure(Exception("El usuario ya es miembro del grupo"))
                }

                // Enviamos notificación
                val notification = Notification(
                    receiver_id = receiverId,
                    sender_id = senderId,
                    group_id = groupId,
                    message = "Te han invitado a unirte a un grupo"
                )
                client.from("notifications").insert(notification)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getNotifications(userId: String): Result<List<Pair<Notification, Group>>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = client.from("notifications")
                    .select(Columns.raw("*, groups(*)")) {
                        filter {
                            eq("receiver_id", userId)
                            eq("status", "pending")
                        }
                    }
                
                val resultList = mutableListOf<Pair<Notification, Group>>()
                val jsonArray = response.data.let { Json.parseToJsonElement(it) }
                
                if (jsonArray is kotlinx.serialization.json.JsonArray) {
                    for (element in jsonArray) {
                        try {
                            val notification = Json.decodeFromJsonElement<Notification>(element)
                            val group = Json.decodeFromJsonElement<Group>(element.jsonObject["groups"]!!)
                            resultList.add(notification to group)
                        } catch (e: Exception) {
                            android.util.Log.e("GroupRepository", "Error decoding notification: ${e.message}")
                        }
                    }
                }
                Result.success(resultList)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun acceptInvitation(notification: Notification): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Añadir al grupo
                val member = GroupMember(
                    group_id = notification.group_id,
                    user_id = notification.receiver_id,
                    status = "active",
                    is_admin = false
                )
                client.from("group_members").insert(member)

                // 2. Marcar notificación como aceptada
                client.from("notifications").update({
                    set("status", "accepted")
                }) {
                    filter {
                        eq("id", notification.id ?: "")
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun declineInvitation(notificationId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("notifications").update({
                    set("status", "declined")
                }) {
                    filter {
                        eq("id", notificationId)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
