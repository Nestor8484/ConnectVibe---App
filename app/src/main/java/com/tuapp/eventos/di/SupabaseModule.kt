package com.tuapp.eventos.di

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.serialization.json.Json

/**
 * Módulo de conexión para Supabase.
 * Recuerda reemplazar los valores de URL y KEY con los de tu proyecto.
 */
object SupabaseModule {
    

    private const val SUPABASE_URL = "https://vpbepmyktlceeuxlgenv.supabase.co"
    

    private const val SUPABASE_KEY = "sb_publishable_6ZS0L38sCDONvpH7Gis0YQ_-St1zV7r"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        httpEngine = OkHttp.create {
            config {
                // Configura aquí si necesitas algo específico de OkHttp
            }
        }
        install(Postgrest)
        install(Auth)
        install(Storage)
        install(Realtime)

        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
}
