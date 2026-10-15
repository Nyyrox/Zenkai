package com.styropyr0.prismal.internal

import android.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Paint
import com.styropyr0.prismal.PrismalShader
import com.styropyr0.prismal.asPrismalShaderHost

internal fun Paint.blur(radius: Float) {
    this.asFrameworkPaint().maskFilter =
        if (radius > 0f) BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        else null
}

internal fun Paint.setAGSLShader(runtimeShader: PrismalShader?) {
    this.asFrameworkPaint().shader = runtimeShader?.asPrismalShaderHost()
}
