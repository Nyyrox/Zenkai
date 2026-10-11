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

import android.content.res.AssetManager
import org.intellij.lang.annotations.Language

/** Loads and caches AGSL shader sources from [ASSET_DIR] in the APK assets. */
internal object PrismalAgslAssets {

    private const val ASSET_DIR = "agsl"

    private lateinit var assetManager: AssetManager
    private val cache = mutableMapOf<String, String>()

    fun bind(assetManager: AssetManager) {
        if (!::assetManager.isInitialized) {
            this.assetManager = assetManager
        }
    }

    @Language("AGSL")
    fun load(name: String, vararg includes: String): String {
        val cacheKey = buildString {
            includes.forEach { append(it).append('|') }
            append(name)
        }
        return cache.getOrPut(cacheKey) {
            buildString {
                includes.forEach { include ->
                    append(readRaw(include))
                    append("\n\n")
                }
                append(readRaw(name))
            }
        }
    }

    private fun readRaw(name: String): String {
        check(::assetManager.isInitialized) {
            "Prismal AGSL assets are not initialized. Ensure App Startup runs before drawing glass."
        }
        return assetManager.open("$ASSET_DIR/$name").bufferedReader().use { it.readText() }
    }
}
