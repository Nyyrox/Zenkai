package com.nyyrox.aniverse.data.download

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Puts a finished episode where the user can actually see it (Files app, VLC, Gallery).
 *
 * Why episodes never showed up outside the app: HLS downloads are a playlist.m3u8 plus hundreds of segments.
 * No other app can play that, so the old publishToGallery() skipped them. This merges the segments into ONE
 * playable .ts / .mp4 on the fly (streamed straight into the destination, no temp copy) and writes it to either
 *   - a folder the user picked (Storage Access Framework), or
 *   - Movies/Zenkai via MediaStore.
 * The in-app copy stays untouched so the Offline Library keeps working.
 */
object DownloadExporter {
    private const val TAG = "DownloadExporter"
    private const val PREFS = "download_export"
    private const val KEY_TREE = "tree_uri"
    private const val KEY_USE_CUSTOM = "use_custom_folder"
    private const val KEY_ASK = "ask_every_time"

    // ── Preferences ─────────────────────────────────────────────────────────────

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Last picked folder, only if we still hold permission for it. */
    fun lastTree(c: Context): Uri? {
        val s = prefs(c).getString(KEY_TREE, null) ?: return null
        val uri = Uri.parse(s)
        val ok = c.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isWritePermission }
        return if (ok) uri else null
    }

    fun persistTree(c: Context, uri: Uri) {
        try {
            c.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            Log.w(TAG, "persist permission failed: ${e.message}")
        }
        prefs(c).edit().putString(KEY_TREE, uri.toString()).apply()
    }

    fun folderLabel(c: Context, uri: Uri?): String? =
        uri?.let { DocumentFile.fromTreeUri(c, it)?.name }

    fun useCustomFolder(c: Context): Boolean = prefs(c).getBoolean(KEY_USE_CUSTOM, false) && lastTree(c) != null
    fun setUseCustomFolder(c: Context, on: Boolean) = prefs(c).edit().putBoolean(KEY_USE_CUSTOM, on).apply()

    fun askEveryTime(c: Context): Boolean = prefs(c).getBoolean(KEY_ASK, false)
    fun setAskEveryTime(c: Context, on: Boolean) = prefs(c).edit().putBoolean(KEY_ASK, on).apply()

    /** Folder used when the user did not pick one for this specific download. */
    fun defaultTree(c: Context): Uri? = if (useCustomFolder(c)) lastTree(c) else null

    // ── Export ──────────────────────────────────────────────────────────────────

    /** "mp4" for fMP4 HLS (has EXT-X-MAP) or a real mp4, otherwise "ts". */
    fun containerFor(finalFile: File): String = when (finalFile.extension.lowercase()) {
        "mp4" -> "mp4"
        "ts" -> "ts"
        else -> if (finalFile.readText().contains("#EXT-X-MAP")) "mp4" else "ts"
    }

    /**
     * Blocking: call from an IO coroutine. Returns a human label of the destination.
     * [tree] != null -> that folder; null -> Movies/Zenkai.
     */
    fun export(context: Context, finalFile: File, displayName: String, tree: Uri?): String {
        val ext = containerFor(finalFile)
        val mime = if (ext == "mp4") "video/mp4" else "video/mp2t"
        val safe = displayName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "episode" }
        val fileName = "$safe.$ext"

        return if (tree != null) {
            val root = DocumentFile.fromTreeUri(context, tree)
                ?: error("Can't open the chosen folder")
            check(root.canWrite()) { "No write access to the chosen folder, pick it again" }
            root.findFile(fileName)?.delete()
            val doc = root.createFile(mime, fileName) ?: error("Can't create file in the chosen folder")
            // Some providers append/rename extensions; force the exact name.
            if (doc.name != fileName) doc.renameTo(fileName)
            try {
                context.contentResolver.openOutputStream(doc.uri, "w")!!.use { writeSingleFile(finalFile, it) }
            } catch (e: Exception) {
                doc.delete() // never leave a broken 0-byte file
                throw e
            }
            "${root.name ?: "chosen folder"}/$fileName"
        } else {
            exportToMovies(context, finalFile, fileName, mime)
            "Movies/Zenkai/$fileName"
        }
    }

    private fun exportToMovies(context: Context, finalFile: File, fileName: String, mime: String) {
        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, mime)
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Zenkai")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore refused the file")
            try {
                resolver.openOutputStream(uri)!!.use { writeSingleFile(finalFile, it) }
                resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Zenkai")
            check(dir.exists() || dir.mkdirs()) { "Can't create ${dir.absolutePath}" }
            val dest = File(dir, fileName)
            dest.outputStream().use { writeSingleFile(finalFile, it) }
            MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf(mime), null)
        }
    }

    fun toast(context: Context, msg: String) {
        Handler(Looper.getMainLooper()).post { Toast.makeText(context.applicationContext, msg, Toast.LENGTH_LONG).show() }
    }

    // ── Merge ───────────────────────────────────────────────────────────────────

    private fun writeSingleFile(finalFile: File, out: OutputStream) {
        if (finalFile.extension.equals("m3u8", ignoreCase = true)) mergeHls(finalFile, out)
        else finalFile.inputStream().use { it.copyTo(out, 256 * 1024) }
        out.flush()
    }

    private class Seg(val file: File, val key: ByteArray?, val iv: ByteArray?)

    /** Resolve a playlist entry written by the downloader: relative path, or just the file name. */
    private fun resolve(dir: File, ref: String): File? {
        val clean = ref.substringBefore('?').substringBefore('#')
        val direct = File(dir, clean)
        if (direct.exists()) return direct
        val byName = File(dir, clean.substringAfterLast('/'))
        return byName.takeIf { it.exists() }
    }

    private fun attr(line: String, name: String): String? =
        Regex("$name=(\"([^\"]*)\"|[^,]*)").find(line)?.let { it.groupValues[2].ifEmpty { it.groupValues[1] } }

    private fun mergeHls(playlist: File, out: OutputStream) {
        val dir = playlist.parentFile ?: error("Bad playlist path")
        var mediaSeq = 0L
        var initFile: File? = null
        var key: ByteArray? = null
        var explicitIv: ByteArray? = null
        val segs = mutableListOf<Seg>()
        var idx = 0L

        for (raw in playlist.readLines()) {
            val line = raw.trim()
            when {
                line.startsWith("#EXT-X-MEDIA-SEQUENCE:") -> mediaSeq = line.substringAfter(':').toLongOrNull() ?: 0L
                line.startsWith("#EXT-X-MAP:") -> {
                    val uri = attr(line, "URI") ?: continue
                    initFile = resolve(dir, uri) ?: error("Missing init segment: $uri")
                }
                line.startsWith("#EXT-X-KEY:") -> {
                    val method = attr(line, "METHOD") ?: "NONE"
                    if (method == "NONE") { key = null; explicitIv = null }
                    else {
                        check(method == "AES-128") { "Unsupported encryption: $method" }
                        val kUri = attr(line, "URI") ?: error("Encrypted stream without key URI")
                        val kFile = resolve(dir, kUri)
                            ?: error("Encrypted stream whose key isn't stored locally")
                        key = kFile.readBytes().also { check(it.size == 16) { "Bad AES key" } }
                        explicitIv = attr(line, "IV")?.removePrefix("0x")?.removePrefix("0X")
                            ?.padStart(32, '0')?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray()
                    }
                }
                line.isNotEmpty() && !line.startsWith("#") -> {
                    val f = resolve(dir, line) ?: error("Missing segment: $line")
                    val iv = if (key == null) null else explicitIv ?: ByteArray(16).also {
                        val n = mediaSeq + idx
                        for (i in 0 until 8) it[15 - i] = (n shr (8 * i)).toByte()
                    }
                    segs += Seg(f, key, iv)
                    idx++
                }
            }
        }
        check(segs.isNotEmpty()) { "Playlist has no segments" }

        initFile?.inputStream()?.use { it.copyTo(out, 256 * 1024) }
        for (s in segs) openSeg(s).use { it.copyTo(out, 256 * 1024) }
    }

    private fun openSeg(s: Seg): InputStream {
        val raw = s.file.inputStream()
        if (s.key == null) return raw
        val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(s.key, "AES"), IvParameterSpec(s.iv))
        return CipherInputStream(raw, c)
    }
}
