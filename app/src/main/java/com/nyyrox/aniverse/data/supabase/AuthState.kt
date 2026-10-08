package com.nyyrox.aniverse.data.supabase

sealed class AuthState {
    data object Loading : AuthState()
    data object LoggedOut : AuthState()
    data class LoggedIn(val userId: String, val email: String?) : AuthState()
    data class Error(val message: String) : AuthState()
}
