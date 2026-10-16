package com.example.storage

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object FileSaver {

    const val FOLDER_DECRYPTED = "تم فك التشفير ✅"
    const val FOLDER_ENCRYPTED = "تم التشفير 🔒"

    /**
     * Saves the processed file to device Downloads folder in the specified subfolder.
     */
    fun saveToDownloads(
        context: Context,
        fileName: String,
        data: ByteArray,
        isDecrypted: Boolean
    ): String {
        val subFolderName = if (isDecrypted) FOLDER_DECRYPTED else FOLDER_ENCRYPTED

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(fileName))
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$subFolderName")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val collectionUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val itemUri = resolver.insert(collectionUri, contentValues)
                ?: throw IllegalStateException("فشل إنشاء ملف جديد في مجلد التنزيلات")

            resolver.openOutputStream(itemUri)?.use { out ->
                out.write(data)
                out.flush()
            }

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(itemUri, contentValues, null, null)

            "Download/$subFolderName/$fileName"
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetDir = File(downloadsDir, subFolderName)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val targetFile = File(targetDir, fileName)
            FileOutputStream(targetFile).use { out ->
                out.write(data)
                out.flush()
            }
            targetFile.absolutePath
        }
    }

    /**
     * Prepares a shareable file in the app cache and returns an ACTION_SEND Intent.
     */
    fun createShareIntent(context: Context, fileName: String, data: ByteArray): Intent {
        val cacheFile = File(context.cacheDir, fileName)
        FileOutputStream(cacheFile).use { it.write(data) }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = getMimeType(fileName)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Writes data directly to a Uri chosen by the user via Storage Access Framework.
     */
    fun writeToUri(context: Context, uri: Uri, data: ByteArray) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(data)
            out.flush()
        } ?: throw IllegalStateException("تعذر فتح مسار الملف المختار للكتابة")
    }

    fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".lua", ignoreCase = true) -> "text/plain"
            fileName.endsWith(".txt", ignoreCase = true) -> "text/plain"
            fileName.endsWith(".json", ignoreCase = true) -> "application/json"
            fileName.endsWith(".zip", ignoreCase = true) -> "application/zip"
            fileName.endsWith(".b64", ignoreCase = true) -> "text/plain"
            else -> "application/octet-stream"
        }
    }
}
