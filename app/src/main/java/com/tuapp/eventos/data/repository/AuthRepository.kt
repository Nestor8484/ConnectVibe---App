package com.tuapp.eventos.data.repository

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
                // 1. Crear el usuario en Auth
                val signUpResponse = client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = pass
                }

                val userId = client.auth.currentUserOrNull()?.id
                
                if (userId != null) {
                    // 2. Crear el perfil en la tabla pública
                    val profile = Profile(
                        id = userId,
                        username = username,
                        full_name = fullName
                    )
                    client.from("profiles").insert(profile)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Error al obtener el ID del usuario"))
                }
            } catch (e: Exception) {
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
