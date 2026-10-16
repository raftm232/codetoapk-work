package com.example.crypto

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class CryptoFormat(val id: String, val titleAr: String, val extension: String) {
    FFSZ("ffsz", "FFSZ (DES + zlib)", ".ffsz"),
    F457("457", "0x457 (XXTEA + zlib)", ".457"),
    B64("b64", "XXTEA + Base64", ".b64")
}

enum class CryptoAction {
    DECRYPT,
    ENCRYPT
}

data class ProcessResult(
    val data: ByteArray,
    val formatDetected: String,
    val originalSize: Int,
    val outputSize: Int,
    val isZip: Boolean,
    val zipEntriesCount: Int = 0,
    val zipProcessedCount: Int = 0,
    val suggestedFileName: String
)

object FfsProcessor {

    fun isZip(data: ByteArray): Boolean {
        if (data.size < 4) return false
        return (data[0] == 0x50.toByte() && data[1] == 0x4B.toByte() &&
                ((data[2] == 3.toByte() && data[3] == 4.toByte()) ||
                 (data[2] == 5.toByte() && data[3] == 6.toByte()) ||
                 (data[2] == 7.toByte() && data[3] == 8.toByte())))
    }

    fun detectAndDecrypt(data: ByteArray, customKey: String? = null): Pair<ByteArray, String> {
        val keyBytes = if (!customKey.isNullOrEmpty()) {
            customKey.toByteArray(Charsets.UTF_8)
        } else {
            null
        }

        if (Ffsz.isFfsz(data)) {
            val decrypted = Ffsz.decrypt(data)
            return Pair(decrypted, "FFSZ")
        }

        if (Ffs457.is457(data)) {
            val decrypted = Ffs457.decrypt(data)
            return Pair(decrypted, "0x457")
        }

        if (XxteaB64.isProbableBase64(data)) {
            try {
                val str = String(data, Charsets.US_ASCII).trim()
                val k = keyBytes ?: XxteaB64.DEFAULT_KEY
                val decrypted = XxteaB64.decodeFromB64(str, k)
                if (decrypted.isNotEmpty()) {
                    return Pair(decrypted, "XXTEA/Base64")
                }
            } catch (_: Exception) {
                // Ignore and fall through to error
            }
        }

        throw IllegalArgumentException("لم يتم التعرف على صيغة التشفير. يدعم فك FFSZ و 0x457 و XXTEA/Base64.")
    }

    fun encrypt(data: ByteArray, format: CryptoFormat, customKey: String? = null): ByteArray {
        val keyBytes = if (!customKey.isNullOrEmpty()) {
            customKey.toByteArray(Charsets.UTF_8)
        } else {
            null
        }

        return when (format) {
            CryptoFormat.FFSZ -> Ffsz.encrypt(data)
            CryptoFormat.F457 -> Ffs457.encrypt(data)
            CryptoFormat.B64 -> {
                val k = keyBytes ?: XxteaB64.DEFAULT_KEY
                XxteaB64.encodeToB64(data, k).toByteArray(Charsets.US_ASCII)
            }
        }
    }

    fun processZip(
        rawZip: ByteArray,
        action: CryptoAction,
        format: CryptoFormat,
        customKey: String? = null,
        onProgress: ((current: Int, total: Int, name: String) -> Unit)? = null
    ): Pair<ByteArray, Pair<Int, Int>> {
        // First pass: count entries
        var totalEntries = 0
        ZipInputStream(ByteArrayInputStream(rawZip)).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                if (!entry.isDirectory) {
                    totalEntries++
                }
                zis.closeEntry()
            }
        }

        val outBuf = ByteArrayOutputStream(rawZip.size)
        var processedCount = 0
        var currentCount = 0

        ZipInputStream(ByteArrayInputStream(rawZip)).use { zis ->
            ZipOutputStream(outBuf).use { zos ->
                while (true) {
                    val entry = zis.nextEntry ?: break
                    val entryName = entry.name

                    if (entry.isDirectory) {
                        val newEntry = ZipEntry(entryName)
                        zos.putNextEntry(newEntry)
                        zos.closeEntry()
                        zis.closeEntry()
                        continue
                    }

                    val content = zis.readBytes()
                    val outputContent: ByteArray = if (action == CryptoAction.DECRYPT) {
                        try {
                            val (decrypted, _) = detectAndDecrypt(content, customKey)
                            processedCount++
                            decrypted
                        } catch (_: Exception) {
                            // Unrecognized file within zip is kept as-is
                            content
                        }
                    } else {
                        processedCount++
                        encrypt(content, format, customKey)
                    }

                    currentCount++
                    onProgress?.invoke(currentCount, totalEntries, entryName)

                    val newEntry = ZipEntry(entryName)
                    zos.putNextEntry(newEntry)
                    zos.write(outputContent)
                    zos.closeEntry()
                    zis.closeEntry()
                }
            }
        }

        return Pair(outBuf.toByteArray(), Pair(totalEntries, processedCount))
    }

    fun process(
        input: ByteArray,
        originalName: String,
        action: CryptoAction,
        format: CryptoFormat,
        customKey: String? = null,
        smartExtension: Boolean = false,
        onProgress: ((current: Int, total: Int, name: String) -> Unit)? = null
    ): ProcessResult {
        if (isZip(input)) {
            val (zipResult, counts) = processZip(input, action, format, customKey, onProgress)
            val suggestedName = if (originalName.lowercase().endsWith(".zip")) {
                originalName
            } else {
                "$originalName.zip"
            }
            return ProcessResult(
                data = zipResult,
                formatDetected = if (action == CryptoAction.DECRYPT) "ZIP (مفحوص)" else "ZIP (${format.titleAr})",
                originalSize = input.size,
                outputSize = zipResult.size,
                isZip = true,
                zipEntriesCount = counts.first,
                zipProcessedCount = counts.second,
                suggestedFileName = suggestedName
            )
        }

        if (action == CryptoAction.DECRYPT) {
            val (decrypted, formatName) = detectAndDecrypt(input, customKey)
            val suggestedName = if (smartExtension) {
                when {
                    originalName.lowercase().endsWith(".457") -> originalName.substring(0, originalName.length - 4) + ".lua"
                    originalName.lowercase().endsWith(".ffsz") -> originalName.substring(0, originalName.length - 5) + ".lua"
                    originalName.lowercase().endsWith(".b64") -> originalName.substring(0, originalName.length - 4) + ".lua"
                    else -> "$originalName.lua"
                }
            } else {
                originalName
            }

            return ProcessResult(
                data = decrypted,
                formatDetected = formatName,
                originalSize = input.size,
                outputSize = decrypted.size,
                isZip = false,
                suggestedFileName = suggestedName
            )
        } else {
            val encrypted = encrypt(input, format, customKey)
            val suggestedName = if (smartExtension) {
                when (format) {
                    CryptoFormat.FFSZ -> if (!originalName.lowercase().endsWith(".ffsz")) "$originalName.ffsz" else originalName
                    CryptoFormat.F457 -> if (!originalName.lowercase().endsWith(".457")) "$originalName.457" else originalName
                    CryptoFormat.B64 -> if (!originalName.lowercase().endsWith(".b64")) "$originalName.b64" else originalName
                }
            } else {
                originalName
            }

            return ProcessResult(
                data = encrypted,
                formatDetected = format.titleAr,
                originalSize = input.size,
                outputSize = encrypted.size,
                isZip = false,
                suggestedFileName = suggestedName
            )
        }
    }
}
