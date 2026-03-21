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
import java.nio.charset.StandardCharsets

/**
 * S3Template 集成测试（基于 Testcontainers 的 MinIO） 验证 S3Template 在实际 S3 兼容存储上的上传与下载功能 使用 MinIO
 * 作为测试容器，确保测试环境可控且可复现 Act - create, put, get Assert Cleanup
 */
@ExtendWith(MinIOTestcontainers::class, SpringExtension::class)
@ContextConfiguration(classes = [S3AutoConfiguration::class])
class S3TemplateIntegrationTest {
    @Resource
    private lateinit var template: S3Template

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
}
