/*
 * Copyright 2025 Styropyr0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.styropyr0.prismal.internal

import org.intellij.lang.annotations.Language

private const val ROUNDED_RECT_SDF = "rounded_rect_sdf.agsl"

@get:Language("AGSL")
internal val PrismalRectRefractionShader: String
    get() = PrismalAgslAssets.load("rect_refraction.agsl", ROUNDED_RECT_SDF)

@get:Language("AGSL")
internal val PrismalRectRefractionDispersionShader: String
    get() = PrismalAgslAssets.load("rect_refraction_dispersion.agsl", ROUNDED_RECT_SDF)

@get:Language("AGSL")
internal val PrismalDefaultSpecularShader: String
    get() = PrismalAgslAssets.load("default_specular.agsl", ROUNDED_RECT_SDF)

@get:Language("AGSL")
internal val PrismalAmbientSpecularShader: String
    get() = PrismalAgslAssets.load("ambient_specular.agsl", ROUNDED_RECT_SDF)

@get:Language("AGSL")
internal val PrismalGradientGlassShader: String
    get() = PrismalAgslAssets.load("gradient_glass.agsl")

@get:Language("AGSL")
internal val PrismalPressRippleShader: String
    get() = PrismalAgslAssets.load("press_ripple.agsl")
