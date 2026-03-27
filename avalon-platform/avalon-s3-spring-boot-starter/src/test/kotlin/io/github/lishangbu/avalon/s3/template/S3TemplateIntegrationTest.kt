package io.github.lishangbu.avalon.s3.template

import io.github.lishangbu.avalon.s3.MinIOTestcontainers
import io.github.lishangbu.avalon.s3.autoconfiguration.S3AutoConfiguration
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * S3Template 集成测试
 *
 * 基于 Testcontainers 启动 MinIO，验证对象与桶的基础读写行为
 */
@ExtendWith(MinIOTestcontainers::class, SpringExtension::class)
@ContextConfiguration(classes = [S3AutoConfiguration::class])
class S3TemplateIntegrationTest {
    /** 注入待测试的 S3Template */
    @Resource
    private lateinit var template: S3Template

    /** 验证可以完成对象上传与下载 */
    @Test
    fun uploadAndDownloadShouldWork() {
        val bucket = "ut-test-bucket"
        val objectKey = "hello.txt"
        val content = "hello-testcontainers"

        template.createBucket(bucket)

        ByteArrayInputStream(content.toByteArray(StandardCharsets.UTF_8)).use { input ->
            template.putObject(bucket, objectKey, input)
        }

        template.getObject(bucket, objectKey).use { downloaded ->
            val restored = downloaded.readAllBytes().toString(StandardCharsets.UTF_8)
            assertEquals(content, restored)
        }

        template.removeObject(bucket, objectKey)
        template.removeBucket(bucket)
    }

    /** 验证上传逻辑不依赖 InputStream.available 返回值 */
    @Test
    fun uploadShouldNotDependOnAvailableByteCount() {
        val bucket = "ut-test-bucket-available"
        val objectKey = "available-zero.txt"
        val content = "available-should-not-control-upload-size"
        val payload = content.toByteArray(StandardCharsets.UTF_8)

        template.createBucket(bucket)

        val input =
            object : InputStream() {
                private val delegate = ByteArrayInputStream(payload)

                override fun read(): Int = delegate.read()

                override fun read(
                    b: ByteArray,
                    off: Int,
                    len: Int,
                ): Int = delegate.read(b, off, len)

                override fun available(): Int = 0
            }

        input.use { template.putObject(bucket, objectKey, it) }

        template.getObject(bucket, objectKey).use { downloaded ->
            val restored = downloaded.readAllBytes().toString(StandardCharsets.UTF_8)
            assertEquals(content, restored)
        }

        template.removeObject(bucket, objectKey)
        template.removeBucket(bucket)
    }

    /** 验证查询桶时缺失桶会返回 null */
    @Test
    fun getBucketShouldReturnNullableBucket() {
        val bucket = "ut-test-bucket-query"

        template.createBucket(bucket)

        val foundBucket = template.getBucket(bucket)
        val missingBucket = template.getBucket("$bucket-missing")

        assertEquals(bucket, foundBucket?.name())
        assertEquals(null, missingBucket)

        template.removeBucket(bucket)
    }
}
