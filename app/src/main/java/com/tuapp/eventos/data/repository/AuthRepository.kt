package com.tuapp.eventos.data.repository

import android.util.Log
import com.tuapp.eventos.data.model.Profile
import com.tuapp.eventos.di.SupabaseModule
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {

    private val client = SupabaseModule.client

    suspend fun signUp(email: String, pass: String, fullName: String, username: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Intentando registro para: $email")
                
                // 1. Crear el usuario en Auth
                val userInfo = client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = pass
                }

                // El ID puede venir directamente de la respuesta del registro o de la sesión actual
                val userId = userInfo?.id ?: client.auth.currentUserOrNull()?.id
                
                Log.d("AuthRepository", "Registro en Auth exitoso. ID: $userId")
                
                if (userId != null) {
                    // 2. Crear el perfil en la tabla pública
                    val profile = Profile(
                        id = userId,
                        username = username,
                        full_name = fullName
                    )
                    Log.d("AuthRepository", "Insertando perfil en DB: $profile")
                    client.from("profiles").insert(profile)
                    Log.d("AuthRepository", "Perfil insertado correctamente")
                    Result.success(Unit)
                } else {
                    Log.e("AuthRepository", "No se pudo obtener el ID del usuario tras el registro")
                    Result.failure(Exception("Error al obtener el ID del usuario"))
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error en signUp: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun signIn(email: String, pass: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Intentando login para: $email")
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = pass
                }
                Log.d("AuthRepository", "Login exitoso")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error en signIn: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            client.auth.signOut()
        }
    }
}
