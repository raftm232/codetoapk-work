package com.example.crypto

import java.nio.ByteBuffer
import java.nio.ByteOrder

object Ffs457 {
    const val MAGIC: Int = 0x00000457
    val KEY: ByteArray = run {
        val k = ByteArray(16)
        val ffs = "ffs".toByteArray(Charsets.US_ASCII)
        System.arraycopy(ffs, 0, k, 0, ffs.size)
        k
    }

    fun is457(blob: ByteArray): Boolean {
        if (blob.size < 8) return false
        val magic = ByteBuffer.wrap(blob, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        return magic == MAGIC
    }

    fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size >= 8) { "حجم الملف صغير جدًا ليكون ملف 0x457" }
        val magic = ByteBuffer.wrap(blob, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        if (magic != MAGIC) {
            throw IllegalArgumentException("ليس ملف 0x457 صحيحًا (الترويسة=0x%08x)".format(magic))
        }

        val ciphertext = ByteArray(blob.size - 4)
        System.arraycopy(blob, 4, ciphertext, 0, ciphertext.size)
        val plain = Xxtea.decryptRaw(ciphertext, KEY)
        if (plain.size < 4) {
            throw IllegalArgumentException("بيانات 0x457 غير صالحة بعد فك التشفير")
        }

        val expectedSize = ByteBuffer.wrap(plain, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val decompressed = try {
            ZlibUtils.decompress(plain, 4, plain.size - 4)
        } catch (_: Exception) {
            if (plain.size >= 8) {
                val zlen = ByteBuffer.wrap(plain, plain.size - 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
                if (zlen > 0 && 4 + zlen <= plain.size) {
                    ZlibUtils.decompress(plain, 4, zlen)
                } else {
                    throw IllegalArgumentException("فشل فك ضغط zlib لملف 0x457")
                }
            } else {
                throw IllegalArgumentException("فشل فك ضغط zlib لملف 0x457")
            }
        }

        return decompressed
    }

    fun encrypt(source: ByteArray, level: Int = 6): ByteArray {
        val zdata = ZlibUtils.compress(source, level)
        val n = source.size
        val rem = (4 + zdata.size) % 4
        val pad = if (rem == 0) 0 else 4 - rem

        val plainLen = 4 + zdata.size + pad + 4
        val plain = ByteArray(plainLen)
        val buf = ByteBuffer.wrap(plain).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(n)
        buf.put(zdata)
        // Pad zeros are already zero in initialized ByteArray
        buf.position(4 + zdata.size + pad)
        buf.putInt(4 + zdata.size)

        val body = Xxtea.encryptRaw(plain, KEY)
        val result = ByteArray(4 + body.size)
        val resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN)
        resBuf.putInt(MAGIC)
        resBuf.put(body)
        return result
    }
}
