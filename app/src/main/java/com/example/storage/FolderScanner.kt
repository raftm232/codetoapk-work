package com.example.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.example.crypto.Ffs457
import com.example.crypto.Ffsz
import com.example.crypto.XxteaB64

data class ScannedFileItem(
    val uri: Uri,
    val documentId: String,
    val name: String,
    val relativePath: String,
    val size: Long,
    val isDirectory: Boolean = false,
    val detectedType: String = "غير محدد",
    val isSelected: Boolean = true
)

object FolderScanner {

    private val SOURCE_EXTENSIONS = setOf(
        "lua", "457", "ffsz", "b64", "txt", "json", "xml", "bin", "dat", "zip"
    )

    /**
     * Recursively scans a document tree Uri for source code and game data files.
     */
    fun scanTree(
        context: Context,
        treeUri: Uri,
        onProgress: ((count: Int, currentName: String) -> Unit)? = null
    ): List<ScannedFileItem> {
        val results = mutableListOf<ScannedFileItem>()
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        scanFolderRecursive(context, treeUri, treeDocId, "", results, onProgress)
        return results
    }

    private fun scanFolderRecursive(
        context: Context,
        treeUri: Uri,
        folderDocId: String,
        currentPath: String,
        results: MutableList<ScannedFileItem>,
        onProgress: ((count: Int, currentName: String) -> Unit)?
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, folderDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idIdx)
                    val name = cursor.getString(nameIdx) ?: "unnamed"
                    val mime = cursor.getString(mimeIdx) ?: ""
                    val size = if (sizeIdx != -1) cursor.getLong(sizeIdx) else 0L

                    val isDir = (mime == DocumentsContract.Document.MIME_TYPE_DIR)
                    val relPath = if (currentPath.isEmpty()) name else "$currentPath/$name"

                    if (isDir) {
                        scanFolderRecursive(context, treeUri, docId, relPath, results, onProgress)
                    } else {
                        val fileDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        val ext = name.substringAfterLast('.', "").lowercase()

                        // Detect format sample
                        val detectedType = detectFileType(context, fileDocUri, ext)

                        val item = ScannedFileItem(
                            uri = fileDocUri,
                            documentId = docId,
                            name = name,
                            relativePath = relPath,
                            size = size,
                            isDirectory = false,
                            detectedType = detectedType,
                            isSelected = true
                        )
                        results.add(item)
                        onProgress?.invoke(results.size, name)
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore subfolder permission errors gracefully
        }
    }

    private fun detectFileType(context: Context, uri: Uri, ext: String): String {
        return try {
            val sample = ByteArray(16)
            val bytesRead = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.read(sample)
            } ?: 0

            if (bytesRead >= 4 && Ffsz.isFfsz(sample)) {
                "FFSZ"
            } else if (bytesRead >= 4 && Ffs457.is457(sample)) {
                "0x457"
            } else if (bytesRead >= 4 && sample[0] == 0x50.toByte() && sample[1] == 0x4B.toByte()) {
                "ZIP"
            } else if (bytesRead >= 8 && XxteaB64.isProbableBase64(sample)) {
                "XXTEA/B64"
            } else if (ext == "lua") {
                "Lua كود"
            } else if (ext in SOURCE_EXTENSIONS) {
                ext.uppercase()
            } else {
                "ملف بيانات"
            }
        } catch (_: Exception) {
            ext.uppercase().ifEmpty { "ملف" }
        }
    }
}
