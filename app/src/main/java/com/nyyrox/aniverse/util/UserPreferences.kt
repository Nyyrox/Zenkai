// UserPreferences.kt - utility for persisting player and user preferences
package com.nyyrox.aniverse.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "user_preferences"

val Context.userPrefsDataStore by preferencesDataStore(name = DATASTORE_NAME)

data class SavedCaptionConfig(
    val fontSizeSp: Float = 18f,
    val textColorArgb: Int = 0xFFFFFFFF.toInt(),
    val bgAlpha: Float = 0.55f,
    val hasShadow: Boolean = true,
    val isBold: Boolean = true,
    val hasStroke: Boolean = true,
    val strokeColorArgb: Int = 0xFF000000.toInt(),
    val strokeWidth: Float = 3.0f
) {
    val textColorLong: Long get() = textColorArgb.toLong() and 0xFFFFFFFFL
    val strokeColorLong: Long get() = strokeColorArgb.toLong() and 0xFFFFFFFFL
}

object UserPreferences {
    private val AUDIO_LANGUAGE = stringPreferencesKey("preferred_audio_language")
    private val SUB_LANGUAGE = stringPreferencesKey("preferred_sub_language")
    private val QUALITY_HEIGHT = intPreferencesKey("preferred_quality_height")
    private val QUALITY_LABEL = stringPreferencesKey("preferred_quality_label")

    private val CAPTIONS_ENABLED = booleanPreferencesKey("captions_enabled")
    private val CAPTION_FONT_SIZE = floatPreferencesKey("caption_font_size")
    private val CAPTION_TEXT_COLOR = intPreferencesKey("caption_text_color_argb")
    private val CAPTION_BG_ALPHA = floatPreferencesKey("caption_bg_alpha")
    private val CAPTION_HAS_SHADOW = booleanPreferencesKey("caption_has_shadow")
    private val CAPTION_IS_BOLD = booleanPreferencesKey("caption_is_bold")
    private val CAPTION_HAS_STROKE = booleanPreferencesKey("caption_has_stroke")
    private val CAPTION_STROKE_COLOR = intPreferencesKey("caption_stroke_color_argb")
    private val CAPTION_STROKE_WIDTH = floatPreferencesKey("caption_stroke_width")

    // --- Audio Language ---
    suspend fun saveAudioLanguage(context: Context, language: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[AUDIO_LANGUAGE] = language
        }
    }

    fun getAudioLanguage(context: Context): Flow<String> =
        context.userPrefsDataStore.data.map { prefs -> prefs[AUDIO_LANGUAGE] ?: "" }

    suspend fun getAudioLanguageSync(context: Context): String =
        try { getAudioLanguage(context).first() } catch (_: Exception) { "" }

    // --- Subtitle Language ---
    suspend fun saveSubLanguage(context: Context, language: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[SUB_LANGUAGE] = language
        }
    }

    fun getSubLanguage(context: Context): Flow<String> =
        context.userPrefsDataStore.data.map { prefs -> prefs[SUB_LANGUAGE] ?: "English" }

    suspend fun getSubLanguageSync(context: Context): String =
        try { getSubLanguage(context).first() } catch (_: Exception) { "English" }

    // --- Video Quality ---
    suspend fun savePreferredQuality(context: Context, height: Int, label: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[QUALITY_HEIGHT] = height
            prefs[QUALITY_LABEL] = label
        }
    }

    fun getPreferredQualityHeight(context: Context): Flow<Int> =
        context.userPrefsDataStore.data.map { prefs -> prefs[QUALITY_HEIGHT] ?: 0 }

    suspend fun getPreferredQualityHeightSync(context: Context): Int =
        try { getPreferredQualityHeight(context).first() } catch (_: Exception) { 0 }

    fun getPreferredQualityLabel(context: Context): Flow<String> =
        context.userPrefsDataStore.data.map { prefs -> prefs[QUALITY_LABEL] ?: "" }

    // --- Caption Settings ---
    suspend fun saveCaptionsEnabled(context: Context, enabled: Boolean) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[CAPTIONS_ENABLED] = enabled
        }
    }

    fun getCaptionsEnabled(context: Context): Flow<Boolean> =
        context.userPrefsDataStore.data.map { prefs -> prefs[CAPTIONS_ENABLED] ?: true }

    fun getCaptionConfig(context: Context): Flow<SavedCaptionConfig> =
        context.userPrefsDataStore.data.map { prefs ->
            SavedCaptionConfig(
                fontSizeSp = prefs[CAPTION_FONT_SIZE] ?: 18f,
                textColorArgb = prefs[CAPTION_TEXT_COLOR] ?: 0xFFFFFFFF.toInt(),
                bgAlpha = prefs[CAPTION_BG_ALPHA] ?: 0.55f,
                hasShadow = prefs[CAPTION_HAS_SHADOW] ?: true,
                isBold = prefs[CAPTION_IS_BOLD] ?: true,
                hasStroke = prefs[CAPTION_HAS_STROKE] ?: true,
                strokeColorArgb = prefs[CAPTION_STROKE_COLOR] ?: 0xFF000000.toInt(),
                strokeWidth = prefs[CAPTION_STROKE_WIDTH] ?: 3.0f
            )
        }

    suspend fun saveCaptionConfig(
        context: Context,
        fontSizeSp: Float,
        textColorArgb: Int,
        bgAlpha: Float,
        hasShadow: Boolean,
        isBold: Boolean,
        hasStroke: Boolean = true,
        strokeColorArgb: Int = 0xFF000000.toInt(),
        strokeWidth: Float = 3.0f
    ) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[CAPTION_FONT_SIZE] = fontSizeSp
            prefs[CAPTION_TEXT_COLOR] = textColorArgb
            prefs[CAPTION_BG_ALPHA] = bgAlpha
            prefs[CAPTION_HAS_SHADOW] = hasShadow
            prefs[CAPTION_IS_BOLD] = isBold
            prefs[CAPTION_HAS_STROKE] = hasStroke
            prefs[CAPTION_STROKE_COLOR] = strokeColorArgb
            prefs[CAPTION_STROKE_WIDTH] = strokeWidth
        }
    }

    // --- Manga Reader Settings ---
    private val MANGA_READER_MODE = stringPreferencesKey("manga_reader_mode")
    private val MANGA_BG_COLOR_INDEX = intPreferencesKey("manga_bg_color_index")
    private val MANGA_PAGE_GAP_INDEX = intPreferencesKey("manga_page_gap_index")

    suspend fun saveMangaReaderSettings(context: Context, mode: String, bgColorIdx: Int, gapIdx: Int) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[MANGA_READER_MODE] = mode
            prefs[MANGA_BG_COLOR_INDEX] = bgColorIdx
            prefs[MANGA_PAGE_GAP_INDEX] = gapIdx
        }
    }

    fun getMangaReaderMode(context: Context): Flow<String> =
        context.userPrefsDataStore.data.map { prefs -> prefs[MANGA_READER_MODE] ?: "VERTICAL_WEBTOON" }

    fun getMangaBgColorIndex(context: Context): Flow<Int> =
        context.userPrefsDataStore.data.map { prefs -> prefs[MANGA_BG_COLOR_INDEX] ?: 0 }

    fun getMangaPageGapIndex(context: Context): Flow<Int> =
        context.userPrefsDataStore.data.map { prefs -> prefs[MANGA_PAGE_GAP_INDEX] ?: 1 }

    // --- Active Novel Source ---
    private val ACTIVE_NOVEL_SOURCE = stringPreferencesKey("active_novel_source")

    suspend fun saveActiveNovelSource(context: Context, sourceId: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[ACTIVE_NOVEL_SOURCE] = sourceId
        }
    }

    fun getActiveNovelSource(context: Context): Flow<String> =
        context.userPrefsDataStore.data.map { prefs -> prefs[ACTIVE_NOVEL_SOURCE] ?: "novelfull" }

    // --- Active Manga Source ---
    private val ACTIVE_MANGA_SOURCE = stringPreferencesKey("active_manga_source")

    suspend fun saveActiveMangaSource(context: Context, sourceId: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[ACTIVE_MANGA_SOURCE] = sourceId
        }
    }

    fun getActiveMangaSource(context: Context): Flow<String> =
        context.userPrefsDataStore.data.map { prefs -> prefs[ACTIVE_MANGA_SOURCE] ?: "weebcentral" }
}
