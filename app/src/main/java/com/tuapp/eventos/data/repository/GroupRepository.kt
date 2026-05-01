package com.tuapp.eventos.data.repository

import android.util.Log
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
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun createGroup(name: String, description: String?, icon: String, color: String, userId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Insert group
                val group = Group(
                    name = name,
                    description = description,
                    icon = icon,
                    color = color,
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

    suspend fun getGroupMembers(groupId: String): Result<List<Pair<GroupMember, Profile>>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("GroupRepository", "Fetching members for group: $groupId")
                val response = client.from("group_members")
                    .select(Columns.raw("*, profiles(*)")) {
                        filter {
                            eq("group_id", groupId)
                        }
                    }
                
                Log.d("GroupRepository", "Raw Response data: ${response.data}")
                
                val resultList = mutableListOf<Pair<GroupMember, Profile>>()
                val jsonArray = json.parseToJsonElement(response.data)
                
                if (jsonArray is kotlinx.serialization.json.JsonArray) {
                    for (element in jsonArray) {
                        try {
                            // Decodificar el miembro (debería ignorar la clave "profiles" por ignoreUnknownKeys)
                            val member = json.decodeFromJsonElement<GroupMember>(element)
                            val profileElement = element.jsonObject["profiles"]
                            
                            // Intentar decodificar el perfil
                            val profile = if (profileElement != null && profileElement !is kotlinx.serialization.json.JsonNull) {
                                try {
                                    if (profileElement is kotlinx.serialization.json.JsonArray) {
                                        if (profileElement.isNotEmpty()) {
                                            json.decodeFromJsonElement<Profile>(profileElement[0])
                                        } else null
                                    } else {
                                        json.decodeFromJsonElement<Profile>(profileElement)
                                    }
                                } catch (e: Exception) {
                                    Log.e("GroupRepository", "Error decoding profile for user ${member.user_id}: ${e.message}")
                                    null
                                }
                            } else {
                                Log.w("GroupRepository", "No profile data for user ${member.user_id} (possible RLS issue)")
                                null
                            }

                            // Siempre añadir el miembro, incluso con perfil fallback
                            val finalProfile = profile ?: Profile(
                                id = member.user_id,
                                full_name = "Usuario desconocido",
                                username = "usuario_${member.user_id.take(5)}"
                            )
                            resultList.add(member to finalProfile)
                        } catch (e: Exception) {
                            Log.e("GroupRepository", "Error decoding member entry: ${e.message}")
                        }
                    }
                }
                Log.d("GroupRepository", "Successfully loaded ${resultList.size} members with profiles")
                Result.success(resultList)
            } catch (e: Exception) {
                Log.e("GroupRepository", "Fatal error in getGroupMembers: ${e.message}", e)
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
                        }
                    }.decodeSingleOrNull<GroupMember>()
                member?.is_admin ?: false
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun getGroupById(groupId: String): Result<Group> {
        return withContext(Dispatchers.IO) {
            try {
                val group = client.from("groups").select {
                    filter {
                        eq("id", groupId)
                    }
                }.decodeSingle<Group>()
                Result.success(group)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getGroupsForUser(userId: String): Result<List<Group>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = client.from("group_members")
                    .select(Columns.raw("groups(*)")) {
                        filter {
                            eq("user_id", userId)
                            eq("status", "active")
                        }
                    }
                
                val resultList = mutableListOf<Group>()
                val jsonArray = json.parseToJsonElement(response.data)
                
                if (jsonArray is kotlinx.serialization.json.JsonArray) {
                    for (element in jsonArray) {
                        try {
                            val groupElement = element.jsonObject["groups"]
                            if (groupElement != null) {
                                val group = json.decodeFromJsonElement<Group>(groupElement)
                                resultList.add(group)
                            }
                        } catch (e: Exception) {
                            Log.e("GroupRepository", "Error decoding group: ${e.message}")
                        }
                    }
                }
                Result.success(resultList)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun searchUsers(query: String): Result<List<Profile>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = client.from("profiles")
                    .select {
                        filter {
                            or {
                                ilike("username", "%$query%")
                                ilike("full_name", "%$query%")
                            }
                        }
                    }.decodeList<Profile>()
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateGroup(groupId: String, name: String, description: String?, icon: String, color: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("groups").update({
                    set("name", name)
                    set("description", description)
                    set("icon", icon)
                    set("color", color)
                }) {
                    filter {
                        eq("id", groupId)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun inviteUserToGroup(groupId: String, receiverId: String, senderId: String): Result<Unit> {
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
                    return@withContext Result.failure(Exception("El usuario ya es miembro de este grupo"))
                }

                // Enviamos notificación
                val notification = Notification(
                    receiver_id = receiverId,
                    sender_id = senderId,
                    group_id = groupId,
                    type = "group_invitation",
                    message = "Invitación a grupo",
                    status = "pending"
                )
                client.from("notifications").insert(notification)
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("GroupRepository", "Error al enviar invitación: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun getNotifications(userId: String): Result<List<Pair<Notification, Group>>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("GroupRepository", "Fetching notifications for user: $userId")
                val response = client.from("notifications")
                    .select(Columns.raw("*, groups(*)")) {
                        filter {
                            eq("receiver_id", userId)
                            eq("status", "pending")
                        }
                    }
                
                val resultList = mutableListOf<Pair<Notification, Group>>()
                val jsonArray = json.parseToJsonElement(response.data)
                
                if (jsonArray is kotlinx.serialization.json.JsonArray) {
                    for (element in jsonArray) {
                        try {
                            val jsonObj = element.jsonObject
                            // Extraer ID de forma segura sin que falle la decodificación si es número
                            val idStr = jsonObj["id"]?.let { 
                                if (it is kotlinx.serialization.json.JsonPrimitive) it.content else it.toString() 
                            }
                            
                            // Decodificar el resto (evitamos conflicto con el ID si es numérico)
                            val notification = json.decodeFromJsonElement<Notification>(
                                kotlinx.serialization.json.JsonObject(jsonObj.filterKeys { it != "id" })
                            ).copy(id = idStr)
                            
                            val groupElement = jsonObj["groups"]
                            if (groupElement != null) {
                                val group = json.decodeFromJsonElement<Group>(groupElement)
                                resultList.add(notification to group)
                            }
                        } catch (e: Exception) {
                            Log.e("GroupRepository", "Error decoding notification: ${e.message}")
                        }
                    }
                }
                Log.d("GroupRepository", "Found ${resultList.size} notifications")
                Result.success(resultList)
            } catch (e: Exception) {
                Log.e("GroupRepository", "Error al obtener notificaciones: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun acceptInvitation(notification: Notification): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("GroupRepository", "Aceptando invitación: Grupo=${notification.group_id}, Usuario=${notification.receiver_id}")
                
                // 1. Intentar añadir al grupo (si ya es miembro, ignoramos el error de inserción)
                try {
                    val member = GroupMember(
                        group_id = notification.group_id,
                        user_id = notification.receiver_id,
                        status = "active",
                        is_admin = false
                    )
                    client.from("group_members").insert(member)
                    Log.d("GroupRepository", "Usuario añadido al grupo correctamente")
                } catch (e: Exception) {
                    Log.w("GroupRepository", "El usuario ya podría ser miembro: ${e.message}")
                }

                // 2. Eliminar la notificación SIEMPRE (aunque la inserción arriba fallara por ya ser miembro)
                val notificationId = notification.id ?: throw Exception("ID de notificación no encontrado")
                val deleteResult = deleteNotification(notificationId)
                
                if (deleteResult.isFailure) {
                    return@withContext Result.failure(deleteResult.exceptionOrNull() ?: Exception("Error al borrar notificación tras aceptar"))
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("GroupRepository", "Error fatal en acceptInvitation: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun declineInvitation(notificationId: String): Result<Unit> {
        return deleteNotification(notificationId)
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("GroupRepository", "Intentando eliminar notificación ID: $notificationId")
                if (notificationId.isBlank()) return@withContext Result.failure(Exception("ID de notificación vacío"))

                val numericId = notificationId.toLongOrNull()
                
                // Realizamos el borrado directo
                client.from("notifications").delete {
                    filter {
                        if (numericId != null) {
                            eq("id", numericId)
                        } else {
                            eq("id", notificationId)
                        }
                    }
                }
                
                Log.d("GroupRepository", "Comando de borrado enviado para ID: $notificationId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("GroupRepository", "Excepción al borrar notificación: ${e.message}")
                Result.failure(e)
            }
        }
    }
}
