package com.example.crypto

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure Kotlin implementation of XXTEA encryption and decryption.
 * Includes both:
 * 1. Raw buffer XXTEA (used by 0x457 format)
 * 2. Cocos2d-x / FunPlus XXTEA (used by xxteaEncodeToB64 / xxteaDecodeFromB64)
 */
object Xxtea {
    private const val DELTA = 0x9E3779B9.toInt()

    @Suppress("NOTHING_TO_INLINE")
    private inline fun mx(sum: Int, y: Int, z: Int, p: Int, e: Int, k: IntArray): Int {
        val part1 = ((z ushr 5) xor (y shl 2)) + ((y ushr 3) xor (z shl 4))
        val part2 = (sum xor y) + (k[(p and 3) xor e] xor z)
        return part1 xor part2
    }

    fun normalizeKey(key: ByteArray): ByteArray {
        val norm = ByteArray(16)
        val len = minOf(key.size, 16)
        System.arraycopy(key, 0, norm, 0, len)
        return norm
    }

    private fun bytesToInts(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): IntArray {
        val count = length / 4
        val buf = ByteBuffer.wrap(data, offset, length).order(ByteOrder.LITTLE_ENDIAN)
        val ints = IntArray(count)
        for (i in 0 until count) {
            ints[i] = buf.int
        }
        return ints
    }

    private fun intsToBytes(ints: IntArray): ByteArray {
        val bytes = ByteArray(ints.size * 4)
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (value in ints) {
            buf.putInt(value)
        }
        return bytes
    }

    /**
     * Decrypt raw buffer where length is a multiple of 4 (used by 0x457)
     */
    fun decryptRaw(data: ByteArray, key: ByteArray): ByteArray {
        require(data.isNotEmpty() && data.size % 4 == 0) {
            "طول بيانات XXTEA يجب أن يكون من مضاعفات 4 وغير صفري"
        }
        val v = bytesToInts(data)
        val k = bytesToInts(normalizeKey(key))
        val n = v.size
        if (n < 2) return data.copyOf()

        val q = 6 + 52 / n
        var sum = q * DELTA
        var y = v[0]

        while (sum != 0) {
            val e = (sum ushr 2) and 3
            for (p in (n - 1) downTo 1) {
                val z = v[p - 1]
                v[p] -= mx(sum, y, z, p, e, k)
                y = v[p]
            }
            val z = v[n - 1]
            v[0] -= mx(sum, y, z, 0, e, k)
            y = v[0]
            sum -= DELTA
        }

        return intsToBytes(v)
    }

    /**
     * Encrypt raw buffer where length is a multiple of 4 (used by 0x457)
     */
    fun encryptRaw(data: ByteArray, key: ByteArray): ByteArray {
        require(data.isNotEmpty() && data.size % 4 == 0) {
            "طول بيانات XXTEA يجب أن يكون من مضاعفات 4 وغير صفري"
        }
        val v = bytesToInts(data)
        val k = bytesToInts(normalizeKey(key))
        val n = v.size
        if (n < 2) return data.copyOf()

        val q = 6 + 52 / n
        var sum = 0
        var z = v[n - 1]

        for (round in 0 until q) {
            sum += DELTA
            val e = (sum ushr 2) and 3
            for (p in 0 until (n - 1)) {
                val y = v[p + 1]
                v[p] += mx(sum, y, z, p, e, k)
                z = v[p]
            }
            val y = v[0]
            v[n - 1] += mx(sum, y, z, n - 1, e, k)
            z = v[n - 1]
        }

        return intsToBytes(v)
    }

    /**
     * Cocos2d-x / FunPlus style XXTEA encrypt:
     * Pads data to multiple of 4, appends data length word, encrypts (n+1) words.
     */
    fun encryptCocos2d(data: ByteArray, key: ByteArray): ByteArray {
        if (data.isEmpty()) return ByteArray(0)
        val rem = data.size % 4
        val pad = if (rem != 0) 4 - rem else 0
        val paddedLen = data.size + pad
        val wordsCount = paddedLen / 4 + 1
        val v = IntArray(wordsCount)

        val buf = ByteBuffer.allocate(paddedLen).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(data)
        buf.position(0)
        for (i in 0 until paddedLen / 4) {
            v[i] = buf.int
        }
        v[wordsCount - 1] = data.size

        val n = v.size - 1
        if (n < 1) return ByteArray(0)
        val k = bytesToInts(normalizeKey(key))

        var z = v[n]
        var sum = 0
        val q = 6 + 52 / (n + 1)
        for (round in 0 until q) {
            sum += DELTA
            val e = (sum ushr 2) and 3
            for (p in 0 until n) {
                val y = v[p + 1]
                v[p] += mx(sum, y, z, p, e, k)
                z = v[p]
            }
            val y = v[0]
            v[n] += mx(sum, y, z, n, e, k)
            z = v[n]
        }

        return intsToBytes(v)
    }

    /**
     * Cocos2d-x / FunPlus style XXTEA decrypt:
     * Decrypts (n+1) words, extracts original length from last word, trims result.
     */
    fun decryptCocos2d(data: ByteArray, key: ByteArray): ByteArray {
        if (data.isEmpty()) return ByteArray(0)
        require(data.size % 4 == 0) { "طول البيانات المشفّرة ليس من مضاعفات 4" }

        val v = bytesToInts(data)
        val n = v.size - 1
        if (n < 1) return ByteArray(0)
        val k = bytesToInts(normalizeKey(key))

        var z = v[n]
        var y = v[0]
        val q = 6 + 52 / (n + 1)
        var sum = q * DELTA

        while (sum != 0) {
            val e = (sum ushr 2) and 3
            for (p in n downTo 1) {
                z = v[p - 1]
                v[p] -= mx(sum, y, z, p, e, k)
                y = v[p]
            }
            z = v[n]
            v[0] -= mx(sum, y, z, 0, e, k)
            y = v[0]
            sum -= DELTA
        }

        val origLen = v[n]
        val allBytes = intsToBytes(v)
        val maxValid = (v.size - 1) * 4
        val minValid = (v.size - 2) * 4
        if (origLen > maxValid || origLen < minValid || origLen < 0) {
            return allBytes
        }
        val result = ByteArray(origLen)
        System.arraycopy(allBytes, 0, result, 0, origLen)
        return result
    }
}
