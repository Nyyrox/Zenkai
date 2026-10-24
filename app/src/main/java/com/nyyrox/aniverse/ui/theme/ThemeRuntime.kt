package com.nyyrox.aniverse.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// ponytail: in-memory only, add DataStore/SharedPreferences persistence when settings need to survive restarts
object ThemeRuntime {
    var cardStyle by mutableStateOf(true)
    var isLight by mutableStateOf(false)
}

fun OttInk(alpha: Float): Color =
    (if (ThemeRuntime.isLight) Color.Black else Color.White).copy(alpha = alpha)
