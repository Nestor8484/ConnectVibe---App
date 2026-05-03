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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

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
                
                Log.d("GroupRepository", "Members response data: ${response.data}")
                
                val jsonElement = json.parseToJsonElement(response.data)
                if (jsonElement !is kotlinx.serialization.json.JsonArray) {
                    return@withContext Result.success(emptyList())
                }

                val resultList = jsonElement.mapNotNull { element ->
                    try {
                        val jsonObj = element.jsonObject
                        
                        // Extraer ID de usuario de forma segura (UUID sin comillas)
                        val userId = jsonObj["user_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null

                        // Mapear Miembro
                        val isAdmin = jsonObj["is_admin"]?.jsonPrimitive?.let { 
                            it.booleanOrNull ?: (it.content == "true") || (it.content == "1")
                        } ?: false
                        
                        val member = GroupMember(
                            group_id = jsonObj["group_id"]?.jsonPrimitive?.contentOrNull ?: groupId,
                            user_id = userId,
                            is_admin = isAdmin,
                            status = jsonObj["status"]?.jsonPrimitive?.contentOrNull ?: "active"
                        )

                        // Mapear Perfil manejando polimorfismo (Objeto o Array)
                        val profilesElement = jsonObj["profiles"]
                        val profileObj = when (profilesElement) {
                            is kotlinx.serialization.json.JsonObject -> profilesElement
                            is kotlinx.serialization.json.JsonArray -> if (profilesElement.isNotEmpty()) profilesElement[0].jsonObject else null
                            else -> null
                        }

                        val profile = if (profileObj != null) {
                            Profile(
                                id = userId,
                                full_name = profileObj["full_name"]?.jsonPrimitive?.contentOrNull,
                                username = profileObj["username"]?.jsonPrimitive?.contentOrNull,
                                avatar_url = profileObj["avatar_url"]?.jsonPrimitive?.contentOrNull
                            )
                        } else {
                            // Fallback si la política de RLS oculta el perfil
                            Profile(
                                id = userId,
                                full_name = "Usuario (Sin acceso)",
                                username = "usuario_${userId.take(5)}"
                            )
                        }
                        
                        member to profile
                    } catch (e: Exception) {
                        Log.e("GroupRepository", "Error processing member row: ${e.message}")
                        null
                    }
                }

                Result.success(resultList)
            } catch (e: Exception) {
                Log.e("GroupRepository", "Fatal error loading members: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun isUserAdmin(groupId: String, userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = client.from("group_members")
                    .select {
                        filter {
                            eq("group_id", groupId)
                            eq("user_id", userId)
                        }
                    }
                
                val jsonElement = json.parseToJsonElement(response.data)
                if (jsonElement is kotlinx.serialization.json.JsonArray && jsonElement.isNotEmpty()) {
                    val jsonObj = jsonElement[0].jsonObject
                    return@withContext jsonObj["is_admin"]?.jsonPrimitive?.let { 
                        it.booleanOrNull ?: (it.content == "true") || (it.content == "1")
                    } ?: false
                }
                false
            } catch (e: Exception) {
                Log.e("GroupRepository", "Error checking admin status: ${e.message}")
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

    suspend fun getAdminGroupsForUser(userId: String): Result<List<Group>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = client.from("group_members")
                    .select(Columns.raw("groups(*)")) {
                        filter {
                            eq("user_id", userId)
                            eq("status", "active")
                            eq("is_admin", true)
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

    suspend fun getNotifications(userId: String): Result<List<Notification>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("GroupRepo_FIX", "Fetching ALL notifications for user: $userId")
                
                // 1. Invitaciones a grupos
                val invitationsResponse = client.from("notifications")
                    .select(Columns.raw("*, groups(name)")) {
                        filter {
                            eq("receiver_id", userId)
                            eq("status", "pending")
                        }
                    }
                
                val invitations = mutableListOf<Notification>()
                val invArray = json.parseToJsonElement(invitationsResponse.data).jsonArray
                Log.d("GroupRepo_FIX", "Found ${invArray.size} raw invitations")
                
                for (element in invArray) {
                    try {
                        val obj = element.jsonObject
                        val idStr = obj["id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else it.toString() }
                        
                        val groupsElement = obj["groups"]
                        val groupObj = when (groupsElement) {
                            is kotlinx.serialization.json.JsonObject -> groupsElement
                            is kotlinx.serialization.json.JsonArray -> if (groupsElement.isNotEmpty()) groupsElement[0].jsonObject else null
                            else -> null
                        }
                        val groupName = groupObj?.get("name")?.jsonPrimitive?.contentOrNull

                        val notif = json.decodeFromJsonElement<Notification>(
                            kotlinx.serialization.json.JsonObject(obj.filterKeys { it != "id" && it != "groups" })
                        ).copy(id = idStr, group_name = groupName, type = "group_invitation")
                        invitations.add(notif)
                    } catch (e: Exception) { Log.e("GroupRepo_FIX", "Error decoding invitation: ${e.message}") }
                }

                // 2. Notificaciones de tareas
                val taskNotifResponse = client.from("notifications_event_tasks")
                    .select(Columns.raw("*, event_tasks(title, events(name))")) {
                        filter {
                            eq("receiver_id", userId)
                            eq("status", "pending")
                        }
                    }
                
                val taskNotifications = mutableListOf<Notification>()
                val taskArray = json.parseToJsonElement(taskNotifResponse.data).jsonArray
                Log.d("GroupRepo_FIX", "Found ${taskArray.size} raw task alerts")
                
                for (element in taskArray) {
                    try {
                        val obj = element.jsonObject
                        val idStr = obj["id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else it.toString() }
                        
                        val tasksElement = obj["event_tasks"]
                        val taskObj = when (tasksElement) {
                            is kotlinx.serialization.json.JsonObject -> tasksElement
                            is kotlinx.serialization.json.JsonArray -> if (tasksElement.isNotEmpty()) tasksElement[0].jsonObject else null
                            else -> null
                        }
                        val taskTitle = taskObj?.get("title")?.jsonPrimitive?.contentOrNull

                        val eventsElement = taskObj?.get("events")
                        val eventObj = when (eventsElement) {
                            is kotlinx.serialization.json.JsonObject -> eventsElement
                            is kotlinx.serialization.json.JsonArray -> if (eventsElement.isNotEmpty()) eventsElement[0].jsonObject else null
                            else -> null
                        }
                        val eventName = eventObj?.get("name")?.jsonPrimitive?.contentOrNull

                        val notif = json.decodeFromJsonElement<Notification>(
                            kotlinx.serialization.json.JsonObject(obj.filterKeys { it != "id" && it != "event_tasks" })
                        ).copy(id = idStr, task_title = taskTitle, event_name = eventName, type = "task_reminder")
                        taskNotifications.add(notif)
                    } catch (e: Exception) { Log.e("GroupRepo_FIX", "Error decoding task alert: ${e.message}") }
                }

                val all = (invitations + taskNotifications).sortedByDescending { it.created_at }
                Log.d("GroupRepo_FIX", "Total notifications to UI: ${all.size}")
                Result.success(all)
            } catch (e: Exception) {
                Log.e("GroupRepo_FIX", "Fatal error loading notifications: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun deleteTaskNotification(notificationId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val numericId = notificationId.toLongOrNull()
                client.from("notifications_event_tasks").delete {
                    filter {
                        if (numericId != null) eq("id", numericId) else eq("id", notificationId)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun acceptInvitation(notification: Notification): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("GroupRepository", "Aceptando invitación: Grupo=${notification.group_id}, Usuario=${notification.receiver_id}")
                
                // 1. Intentar añadir al grupo (si ya es miembro, ignoramos el error de inserción)
                try {
                    val groupId = notification.group_id ?: throw Exception("ID de grupo no encontrado en la notificación")
                    val member = GroupMember(
                        group_id = groupId,
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

    suspend fun updateMemberRole(groupId: String, userId: String, isAdmin: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("GroupRepository", "Updating member role: group=$groupId, user=$userId, isAdmin=$isAdmin")
                client.from("group_members").update(
                    kotlinx.serialization.json.buildJsonObject {
                        put("is_admin", isAdmin)
                    }
                ) {
                    filter {
                        eq("group_id", groupId)
                        eq("user_id", userId)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("GroupRepository", "Error updating member role: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun removeMember(groupId: String, userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Obtener todos los eventos de este grupo
                val eventsResponse = client.from("events")
                    .select(io.github.jan.supabase.postgrest.query.Columns.raw("id")) {
                        filter {
                            eq("group_id", groupId)
                        }
                    }
                
                val eventsArray = json.parseToJsonElement(eventsResponse.data).jsonArray
                val eventIds = eventsArray.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }

                if (eventIds.isNotEmpty()) {
                    // 2. Eliminar al usuario de los roles en esos eventos
                    client.from("event_role_members").delete {
                        filter {
                            eq("user_id", userId)
                            or {
                                eventIds.forEach { id ->
                                    eq("event_id", id)
                                }
                            }
                        }
                    }
                    
                    // 3. Eliminar al usuario de los miembros de esos eventos
                    client.from("event_members").delete {
                        filter {
                            eq("user_id", userId)
                            or {
                                eventIds.forEach { id ->
                                    eq("event_id", id)
                                }
                            }
                        }
                    }
                }

                // 4. Finalmente, eliminar del grupo
                client.from("group_members").delete {
                    filter {
                        eq("group_id", groupId)
                        eq("user_id", userId)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("GroupRepository", "Error removing member: ${e.message}")
                Result.failure(e)
            }
        }
    }
}
