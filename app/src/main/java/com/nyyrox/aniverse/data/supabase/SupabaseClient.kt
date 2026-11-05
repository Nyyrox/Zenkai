package com.nyyrox.aniverse.data.supabase

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseManager {
    const val SUPABASE_URL = "https://wcvazatblggnbnrguopj.supabase.co"
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndjdmF6YXRibGdnbmJucmd1b3BqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTI1MDIwNDYsImV4cCI6MjEwODA3ODA0Nn0.RaySQctuclhida6PinKXXCDSFeRwN_N7PzkbyGwugqk"

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
