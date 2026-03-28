package io.github.lishangbu.avalon.s3.facade

import io.github.lishangbu.avalon.s3.MinIOTestcontainers
import io.github.lishangbu.avalon.s3.autoconfiguration.S3AutoConfiguration
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import software.amazon.awssdk.services.s3.model.CompletedPart
import java.nio.charset.StandardCharsets
import java.time.Duration

/** 验证 facade 可通过 MinIO 访问基础数据面能力。 */
@ExtendWith(MinIOTestcontainers::class, SpringExtension::class)
@ContextConfiguration(classes = [S3AutoConfiguration::class])
class S3FacadeIntegrationTest {
    @Resource
    private lateinit var s3Facade: S3Facade

    @Test
    fun shouldUploadDownloadAndPresignObjects() {
        val bucketName = "ut-s3-facade-bucket"
        val key = "hello.txt"
        val content = "hello-avalon-s3".toByteArray(StandardCharsets.UTF_8)

        s3Facade.buckets.create(bucketName)
        s3Facade.buckets.waitUntilExists(bucketName)
        s3Facade.objects.put(bucketName, key, content, "text/plain")

        val bytes = s3Facade.objects.getBytes(bucketName, key).asByteArray()
        assertArrayEquals(content, bytes)

        val presignedUrl =
            s3Facade.presign
                .get(bucketName, key, Duration.ofMinutes(5))
                .url()
                .toString()
        assertEquals(true, presignedUrl.contains(bucketName))

        s3Facade.objects.delete(bucketName, key)
        s3Facade.buckets.delete(bucketName)
    }

    @Test
    fun shouldSupportMultipartUpload() {
        val bucketName = "ut-s3-multipart-bucket"
        val key = "multipart.txt"
        val partOne = ByteArray(5 * 1024 * 1024) { 'a'.code.toByte() }
        val partTwo = "world".toByteArray(StandardCharsets.UTF_8)

        s3Facade.buckets.create(bucketName)
        val upload = s3Facade.multipart.create(bucketName, key, "text/plain")

        val completedParts =
            listOf(
                s3Facade.multipart.uploadPart(bucketName, key, upload.uploadId(), 1, partOne),
                s3Facade.multipart.uploadPart(bucketName, key, upload.uploadId(), 2, partTwo),
            ).mapIndexed { index, response ->
                CompletedPart
                    .builder()
                    .partNumber(index + 1)
                    .eTag(response.eTag())
                    .build()
            }

        s3Facade.multipart.complete(bucketName, key, upload.uploadId(), completedParts)

        val restored = s3Facade.objects.getBytes(bucketName, key).asByteArray()
        assertEquals(partOne.size + partTwo.size, restored.size)
        assertEquals('a'.code.toByte(), restored.first())
        assertEquals("world", restored.copyOfRange(partOne.size, restored.size).toString(StandardCharsets.UTF_8))

        s3Facade.objects.delete(bucketName, key)
        s3Facade.buckets.delete(bucketName)
    }
}
