package com.example

import com.example.crypto.CryptoAction
import com.example.crypto.CryptoFormat
import com.example.crypto.Ffs457
import com.example.crypto.FfsProcessor
import com.example.crypto.Ffsz
import com.example.crypto.XxteaB64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExampleUnitTest {

    @Test
    fun testFfs457RoundTrip() {
        val original = "function helloWorld() print('Welcome to Family Farm Seaside!') end".toByteArray(Charsets.UTF_8)
        val encrypted = Ffs457.encrypt(original)

        assertTrue("Should detect 0x457 magic", Ffs457.is457(encrypted))
        val decrypted = Ffs457.decrypt(encrypted)
        assertArrayEquals("Decrypted 0x457 must equal original", original, decrypted)
    }

    @Test
    fun testFfszRoundTrip() {
        val original = "local farmData = { coins = 999999, level = 100, cash = 5000 } return farmData".toByteArray(Charsets.UTF_8)
        val encrypted = Ffsz.encrypt(original)

        assertTrue("Should detect FFSZ magic", Ffsz.isFfsz(encrypted))
        val decrypted = Ffsz.decrypt(encrypted)
        assertArrayEquals("Decrypted FFSZ must equal original", original, decrypted)
    }

    @Test
    fun testXxteaB64RoundTrip() {
        val original = "local testScript = 'FunPlus xxtea b64 test'".toByteArray(Charsets.UTF_8)
        val b64 = XxteaB64.encodeToB64(original)
        assertTrue("Output should be Base64", b64.isNotEmpty())

        val decrypted = XxteaB64.decodeFromB64(b64)
        assertArrayEquals("Decrypted B64 must equal original", original, decrypted)
    }

    @Test
    fun testAutoDetectAndDecrypt() {
        val luaCode = "print('auto detect test')".toByteArray(Charsets.UTF_8)

        // 1. FFSZ
        val ffszEnc = Ffsz.encrypt(luaCode)
        val (ffszDec, ffszType) = FfsProcessor.detectAndDecrypt(ffszEnc)
        assertEquals("FFSZ", ffszType)
        assertArrayEquals(luaCode, ffszDec)

        // 2. 0x457
        val f457Enc = Ffs457.encrypt(luaCode)
        val (f457Dec, f457Type) = FfsProcessor.detectAndDecrypt(f457Enc)
        assertEquals("0x457", f457Type)
        assertArrayEquals(luaCode, f457Dec)

        // 3. Base64
        val b64Enc = XxteaB64.encodeToB64(luaCode).toByteArray(Charsets.US_ASCII)
        val (b64Dec, b64Type) = FfsProcessor.detectAndDecrypt(b64Enc)
        assertEquals("XXTEA/Base64", b64Type)
        assertArrayEquals(luaCode, b64Dec)
    }

    @Test
    fun testZipProcessing() {
        // Create sample zip with an FFSZ file and a 0x457 file
        val file1Original = "print('script1')".toByteArray(Charsets.UTF_8)
        val file2Original = "print('script2')".toByteArray(Charsets.UTF_8)

        val file1Enc = Ffsz.encrypt(file1Original)
        val file2Enc = Ffs457.encrypt(file2Original)

        val zipOut = ByteArrayOutputStream()
        ZipOutputStream(zipOut).use { zos ->
            zos.putNextEntry(ZipEntry("scripts/file1.ffsz"))
            zos.write(file1Enc)
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("scripts/file2.457"))
            zos.write(file2Enc)
            zos.closeEntry()
        }

        val zipBytes = zipOut.toByteArray()
        val result = FfsProcessor.process(
            input = zipBytes,
            originalName = "test_archive.zip",
            action = CryptoAction.DECRYPT,
            format = CryptoFormat.FFSZ
        )

        assertTrue(result.isZip)
        assertEquals(2, result.zipEntriesCount)
        assertEquals(2, result.zipProcessedCount)
    }

    @Test
    fun testApkConversionStages() {
        val stages = com.example.crypto.ApkStage.entries
        assertTrue("Must include Compressing stage", stages.any { it.stageTag == "Compressing" })
        assertTrue("Must include Signing stage", stages.any { it.stageTag == "Signing" })
        assertTrue("Must include Finalizing stage", stages.any { it.stageTag == "Finalizing" })

        val compressing = com.example.crypto.ApkStage.COMPRESSING
        assertEquals("Compressing", compressing.stageTag)

        val signing = com.example.crypto.ApkStage.SIGNING
        assertEquals("Signing", signing.stageTag)

        val finalizing = com.example.crypto.ApkStage.FINALIZING
        assertEquals("Finalizing", finalizing.stageTag)
    }
}
