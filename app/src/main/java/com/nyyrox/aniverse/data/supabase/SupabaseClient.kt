package com.nyyrox.aniverse.data.supabase

import android.content.Context
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SupabaseManager {
    const val SUPABASE_URL = "https://wcvazatblggnbnrguopj.supabase.co"
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndjdmF6YXRibGdnbmJucmd1b3BqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTI1MDIwNDYsImV4cCI6MjEwODA3ODA0Nn0.RaySQctuclhida6PinKXXCDSFeRwN_N7PzkbyGwugqk"

    @Volatile
    var appContext: Context? = null

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    class AndroidSessionManager : SessionManager {
        private var inMemorySession: UserSession? = null

        override suspend fun saveSession(session: UserSession) {
            inMemorySession = session
            val ctx = appContext
            if (ctx != null) {
                try {
                    val prefs = ctx.getSharedPreferences("aniverse_storage", Context.MODE_PRIVATE)
                    prefs.edit().putString("supabase_auth_session", json.encodeToString(session)).apply()
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseManager", "saveSession error", e)
                }
            }
        }

        override suspend fun loadSession(): UserSession? {
            if (inMemorySession != null) return inMemorySession
            val ctx = appContext ?: return null
            return try {
                val prefs = ctx.getSharedPreferences("aniverse_storage", Context.MODE_PRIVATE)
                val raw = prefs.getString("supabase_auth_session", null) ?: return null
                val session = json.decodeFromString<UserSession>(raw)
                inMemorySession = session
                session
            } catch (e: Exception) {
                android.util.Log.e("SupabaseManager", "loadSession error", e)
                null
            }
        }

        override suspend fun deleteSession() {
            inMemorySession = null
            val ctx = appContext
            if (ctx != null) {
                try {
                    val prefs = ctx.getSharedPreferences("aniverse_storage", Context.MODE_PRIVATE)
                    prefs.edit().remove("supabase_auth_session").apply()
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseManager", "deleteSession error", e)
                }
            }
        }
    }

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = ANON_KEY
    ) {
        defaultSerializer = KotlinXSerializer(json)
        install(Auth) {
            flowType = FlowType.PKCE
            scheme = "aniverse"
            host = "login-callback"
            sessionManager = AndroidSessionManager()
        }
        install(Postgrest)
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
