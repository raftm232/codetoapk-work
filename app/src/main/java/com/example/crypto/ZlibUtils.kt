package com.example.crypto

import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

object ZlibUtils {
    /**
     * Decompress standard zlib stream (RFC 1950).
     */
    fun decompress(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): ByteArray {
        val inflater = Inflater(false)
        inflater.setInput(data, offset, length)
        val out = ByteArrayOutputStream(maxOf(length * 2, 1024))
        val buffer = ByteArray(8192)
        try {
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0) {
                    if (inflater.needsInput()) break
                    if (inflater.needsDictionary()) throw DataFormatException("Zlib needs dictionary")
                } else {
                    out.write(buffer, 0, count)
                }
            }
            return out.toByteArray()
        } finally {
            inflater.end()
        }
    }

    /**
     * Compress data with zlib (RFC 1950, default level = 6).
     */
    fun compress(data: ByteArray, level: Int = 6): ByteArray {
        val deflater = Deflater(level, false)
        deflater.setInput(data)
        deflater.finish()
        val out = ByteArrayOutputStream(maxOf(data.size / 2, 1024))
        val buffer = ByteArray(8192)
        try {
            while (!deflater.finished()) {
                val count = deflater.deflate(buffer)
                out.write(buffer, 0, count)
            }
            return out.toByteArray()
        } finally {
            deflater.end()
        }
    }
}
