package com.example.crypto

/**
 * XXTEA + Base64 codec following FunPlus / cocos2d-x pattern.
 * Uses a self-contained Base64 implementation compatible with both JVM tests and Android API 24+.
 */
object XxteaB64 {
    val DEFAULT_KEY: ByteArray = "ffs".toByteArray(Charsets.UTF_8)

    private const val B64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun encodeToB64(plain: ByteArray, key: ByteArray = DEFAULT_KEY): String {
        val encrypted = Xxtea.encryptCocos2d(plain, key)
        return b64Encode(encrypted)
    }

    fun decodeFromB64(b64Text: String, key: ByteArray = DEFAULT_KEY): ByteArray {
        val clean = b64Text.trim()
        val decoded = b64Decode(clean)
        return Xxtea.decryptCocos2d(decoded, key)
    }

    fun isProbableBase64(data: ByteArray): Boolean {
        if (data.isEmpty()) return false
        var nonWhitespaceCount = 0
        for (b in data) {
            val c = b.toInt().toChar()
            if (c == '\r' || c == '\n' || c == ' ' || c == '\t') continue
            val valid = (c in 'A'..'Z') || (c in 'a'..'z') || (c in '0'..'9') || c == '+' || c == '/' || c == '='
            if (!valid) return false
            nonWhitespaceCount++
        }
        return nonWhitespaceCount > 0 && nonWhitespaceCount % 4 == 0
    }

    private fun b64Encode(data: ByteArray): String {
        val sb = StringBuilder((data.size * 4 + 2) / 3)
        var i = 0
        while (i < data.size) {
            val b0 = data[i].toInt() and 0xFF
            val b1 = if (i + 1 < data.size) data[i + 1].toInt() and 0xFF else -1
            val b2 = if (i + 2 < data.size) data[i + 2].toInt() and 0xFF else -1

            val c0 = b0 ushr 2
            val c1 = ((b0 and 0x03) shl 4) or if (b1 != -1) (b1 ushr 4) else 0
            val c2 = if (b1 != -1) (((b1 and 0x0F) shl 2) or if (b2 != -1) (b2 ushr 6) else 0) else -1
            val c3 = if (b2 != -1) (b2 and 0x3F) else -1

            sb.append(B64_CHARS[c0])
            sb.append(B64_CHARS[c1])
            sb.append(if (c2 != -1) B64_CHARS[c2] else '=')
            sb.append(if (c3 != -1) B64_CHARS[c3] else '=')

            i += 3
        }
        return sb.toString()
    }

    private fun b64Decode(str: String): ByteArray {
        val clean = StringBuilder()
        for (ch in str) {
            if (ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' || ch == '+' || ch == '/') {
                clean.append(ch)
            } else if (ch == '=') {
                break
            }
        }
        val len = clean.length
        val outLen = (len * 6) / 8
        val out = ByteArray(outLen)
        var bitBuf = 0
        var bits = 0
        var outIdx = 0

        for (i in 0 until len) {
            val c = clean[i]
            val v = when (c) {
                in 'A'..'Z' -> c - 'A'
                in 'a'..'z' -> c - 'a' + 26
                in '0'..'9' -> c - '0' + 52
                '+' -> 62
                '/' -> 63
                else -> 0
            }
            bitBuf = (bitBuf shl 6) or v
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out[outIdx++] = ((bitBuf ushr bits) and 0xFF).toByte()
            }
        }
        return if (outIdx == out.size) out else out.copyOf(outIdx)
    }
}
