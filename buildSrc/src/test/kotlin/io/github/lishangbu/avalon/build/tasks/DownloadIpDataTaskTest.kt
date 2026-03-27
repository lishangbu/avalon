package io.github.lishangbu.avalon.build.tasks

import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DownloadIpDataTaskTest {
    @Test
    fun infersOfficialPackageCodeFromIpv6BinName() {
        assertEquals("DB11LITEBINIPV6", inferOfficialIp2LocationPackageCode("IP2LOCATION-LITE-DB11.IPV6.BIN"))
    }

    @Test
    fun infersOfficialPackageCodeFromIpv4BinName() {
        assertEquals("DB1LITEBIN", inferOfficialIp2LocationPackageCode("IP2LOCATION-LITE-DB1.BIN"))
    }

    @Test
    fun rejectsUnsupportedFileName() {
        assertNull(inferOfficialIp2LocationPackageCode("custom.bin"))
    }

    @Test
    fun buildsOfficialDownloadUrl() {
        assertEquals(
            "https://www.ip2location.com/download?token=test-token&file=DB11LITEBINIPV6",
            officialIp2LocationDownloadUrl("test-token", "DB11LITEBINIPV6"),
        )
    }

    @Test
    fun extractsMatchingFileFromZipStream() {
        val zipFile = createTempFile(suffix = ".zip").toFile()
        FileOutputStream(zipFile).use { fileOutputStream ->
            ZipOutputStream(fileOutputStream).use { zipOutputStream ->
                zipOutputStream.putNextEntry(ZipEntry("nested/IP2LOCATION-LITE-DB11.IPV6.BIN"))
                zipOutputStream.write("db-content".toByteArray())
                zipOutputStream.closeEntry()
            }
        }
        val destinationFile = createTempFile(suffix = ".bin").toFile()

        zipFile.inputStream().use { inputStream ->
            ZipInputStream(inputStream.buffered()).use { zipInputStream ->
                assertTrue(
                    extractFileFromZipStream(
                        zipInputStream,
                        "IP2LOCATION-LITE-DB11.IPV6.BIN",
                        destinationFile,
                    ),
                )
            }
        }

        assertEquals("db-content", destinationFile.readText())
    }

    @Test
    fun returnsFalseWhenZipDoesNotContainRequestedFile() {
        val zipFile = createTempFile(suffix = ".zip").toFile()
        FileOutputStream(zipFile).use { fileOutputStream ->
            ZipOutputStream(fileOutputStream).use { zipOutputStream ->
                zipOutputStream.putNextEntry(ZipEntry("nested/other.bin"))
                zipOutputStream.write("other".toByteArray())
                zipOutputStream.closeEntry()
            }
        }
        val destinationFile = createTempFile(suffix = ".bin").toFile()

        zipFile.inputStream().use { inputStream ->
            ZipInputStream(inputStream.buffered()).use { zipInputStream ->
                assertFalse(
                    extractFileFromZipStream(
                        zipInputStream,
                        "IP2LOCATION-LITE-DB11.IPV6.BIN",
                        destinationFile,
                    ),
                )
            }
        }
    }
}
