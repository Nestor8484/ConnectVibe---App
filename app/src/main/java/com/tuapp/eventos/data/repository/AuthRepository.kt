package com.tuapp.eventos.data.repository

import android.util.Log
import com.tuapp.eventos.data.model.Profile
import com.tuapp.eventos.di.SupabaseModule
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {

    private val client = SupabaseModule.client

    suspend fun signUp(email: String, pass: String, fullName: String, username: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Iniciando registro para: $email con username: $username")
                
                // 1. Crear el usuario en Supabase Auth con metadatos
                val userResponse = client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = pass
                    data = buildJsonObject {
                        put("username", username)
                        put("full_name", fullName)
                    }
                }

                val userId = userResponse?.id ?: client.auth.currentUserOrNull()?.id
                
                if (userId == null) {
                    Log.w("AuthRepository", "Registro exitoso en Auth, pero no se obtuvo ID inmediata (posible confirmación de email requerida)")
                    return@withContext Result.success(Unit)
                }

                // 2. Intentar crear el perfil en la tabla 'profiles' para búsquedas posteriores
                try {
                    val profile = Profile(
                        id = userId,
                        username = username,
                        full_name = fullName,
                        email = email // Guardar el email es clave para el login por username
                    )
                    client.from("profiles").upsert(profile)
                    Log.d("AuthRepository", "Perfil de usuario creado exitosamente")
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Error al insertar perfil en DB: ${e.message}")
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error en signUp: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun signIn(identifier: String, pass: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanIdentifier = identifier.trim()
                
                // Si el identificador NO es un email, buscamos el email asociado al username
                val targetEmail = if (android.util.Patterns.EMAIL_ADDRESS.matcher(cleanIdentifier).matches()) {
                    cleanIdentifier
                } else {
                    Log.d("AuthRepository", "Buscando email para el username: $cleanIdentifier")
                    
                    val profile = try {
                        client.from("profiles")
                            .select {
                                filter { 
                                    // Usamos ilike para que no importe mayúsculas/minúsculas
                                    ilike("username", cleanIdentifier) 
                                }
                            }
                            .decodeSingleOrNull<Profile>()
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Error al consultar la tabla profiles: ${e.message}")
                        null
                    }

                    if (profile == null) {
                        throw Exception("No se encontró el usuario '$cleanIdentifier'. Revisa el nombre o usa tu email.")
                    }
                    
                    profile.email ?: throw Exception("El perfil no tiene un correo electrónico asociado.")
                }

                Log.d("AuthRepository", "Intentando login para: $targetEmail")
                client.auth.signInWith(Email) {
                    this.email = targetEmail
                    this.password = pass
                }
                
                Log.d("AuthRepository", "Login exitoso")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error en signIn: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            try {
                client.auth.signOut()
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error al cerrar sesión: ${e.message}")
            }
        }
    }

    suspend fun updateProfile(fullName: String, username: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = client.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val profile = Profile(
                        id = userId,
                        username = username,
                        full_name = fullName
                    )
                    client.from("profiles").upsert(profile)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Usuario no autenticado"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.updateUser {
                    password = newPassword
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getCurrentProfile(): Result<Profile> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = client.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val profile = client.from("profiles")
                        .select {
                            filter { eq("id", userId) }
                        }
                        .decodeSingle<Profile>()
                    Result.success(profile)
                } else {
                    Result.failure(Exception("Usuario no autenticado"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun getCurrentUserEmail(): String? = client.auth.currentUserOrNull()?.email
}
