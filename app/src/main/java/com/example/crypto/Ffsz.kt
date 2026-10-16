package com.example.crypto

import java.nio.ByteBuffer
import java.nio.ByteOrder

object Ffsz {
    val MAGIC: ByteArray = "FFSZ".toByteArray(Charsets.US_ASCII)
    val KEY: ByteArray = "FP_F_ENC".toByteArray(Charsets.US_ASCII)

    fun isFfsz(blob: ByteArray): Boolean {
        if (blob.size < 12) return false
        return blob[0] == 'F'.code.toByte() &&
                blob[1] == 'F'.code.toByte() &&
                blob[2] == 'S'.code.toByte() &&
                blob[3] == 'Z'.code.toByte()
    }

    fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size >= 12) { "الملف قصير جدًا ليكون ملف FFSZ" }
        if (!isFfsz(blob)) {
            throw IllegalArgumentException("ليست صيغة FFSZ (الترويسة غير مطابقة)")
        }

        val lengthLong = ByteBuffer.wrap(blob, 4, 8).order(ByteOrder.LITTLE_ENDIAN).long
        if (lengthLong < 8L || lengthLong > (blob.size - 12)) {
            throw IllegalArgumentException("طول بيانات FFSZ غير صالح: $lengthLong")
        }
        val length = lengthLong.toInt()

        val ct = ByteArray(blob.size - 12)
        System.arraycopy(blob, 12, ct, 0, ct.size)
        val decrypted = DesCipher.process(ct, KEY, decrypt = true)

        if (decrypted.size < length || length < 8) {
            throw IllegalArgumentException("البيانات بعد فك تشفير DES أقصر من المطلوب")
        }

        // pt layout: [0:4] uint32 uncompressed size, [4:8] reserved=0, [8:] zlib stream
        return ZlibUtils.decompress(decrypted, 8, length - 8)
    }

    fun encrypt(source: ByteArray, level: Int = 6): ByteArray {
        val zdata = ZlibUtils.compress(source, level)
        val plainLen = 8 + zdata.size
        val rem = plainLen % 8
        val pad = if (rem == 0) 0 else 8 - rem
        val totalPlain = plainLen + pad

        val plain = ByteArray(totalPlain)
        val buf = ByteBuffer.wrap(plain).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(source.size) // uint32 LE uncompressed size
        buf.putInt(0)           // uint32 reserved = 0
        buf.put(zdata)

        val ct = DesCipher.process(plain, KEY, decrypt = false)

        val result = ByteArray(4 + 8 + ct.size)
        val resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN)
        resBuf.put(MAGIC)
        resBuf.putLong(plainLen.toLong())
        resBuf.put(ct)

        return result
    }
}
