package com.nyyrox.aniverse.data.supabase

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseManager {
    const val SUPABASE_URL = "https://voxeodunvjrhtbjvbbxt.supabase.co"
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZveGVvZHVudmpyaHRianZiYnh0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg3NjE0MzYsImV4cCI6MjEwNDMzNzQzNn0.wtggmDZQgKMa_PVNpX9g-gUoFn6BgjCJ9FMf77zctlg"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = ANON_KEY
    ) {
        install(Auth) {
            flowType = FlowType.PKCE
            scheme = "aniverse"
            host = "login-callback"
        }
        install(Postgrest)
    }
}
