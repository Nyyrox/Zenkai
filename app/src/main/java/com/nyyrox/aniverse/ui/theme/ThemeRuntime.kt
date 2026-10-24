package com.nyyrox.aniverse.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class ThemeMode { SYSTEM, LIGHT, DARK }

// in-memory only, add DataStore/SharedPreferences persistence when settings need to survive restarts
object ThemeRuntime {
    var cardStyle by mutableStateOf(true)
    var isLight by mutableStateOf(false)
    var themeMode by mutableStateOf(ThemeMode.DARK)
        private set

    // call with enum
    fun setThemeMode(mode: ThemeMode, systemIsDark: Boolean = true) {
        themeMode = mode
        isLight = when (mode) {
            ThemeMode.LIGHT -> true
            ThemeMode.DARK -> false
            ThemeMode.SYSTEM -> !systemIsDark
        }
    }

    // call with boolean (true = light)
    fun setThemeMode(light: Boolean) {
        themeMode = if (light) ThemeMode.LIGHT else ThemeMode.DARK
        isLight = light
    }
}

fun OttInk(alpha: Float): Color =
    (if (ThemeRuntime.isLight) Color.Black else Color.White).copy(alpha = alpha)
