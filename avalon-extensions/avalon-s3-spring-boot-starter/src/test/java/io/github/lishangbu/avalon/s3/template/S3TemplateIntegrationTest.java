package io.github.lishangbu.avalon.s3.template;

import io.github.lishangbu.avalon.s3.MinIOTestcontainers;
import io.github.lishangbu.avalon.s3.autoconfiguration.S3AutoConfiguration;
import jakarta.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/// S3Template 集成测试（基于 Testcontainers 的 MinIO）
/// 验证 S3Template 在实际 S3 兼容存储上的上传与下载功能
/// 使用 MinIO 作为测试容器，确保测试环境可控且可复现
@ExtendWith({MinIOTestcontainers.class, SpringExtension.class})
@ContextConfiguration(classes = {S3AutoConfiguration.class})
class S3TemplateIntegrationTest {

    @Resource private S3Template template;

    @Test
    void uploadAndDownloadShouldWork() throws Exception {
        String bucket = "ut-test-bucket";
        String objectKey = "hello.txt";
        String content = "hello-testcontainers";

        // Act - create, put, get
        template.createBucket(bucket);

        try (InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            template.putObject(bucket, objectKey, in);
        }

        try (InputStream downloaded = template.getObject(bucket, objectKey)) {
            byte[] data = downloaded.readAllBytes();
            String restored = new String(data, StandardCharsets.UTF_8);

            // Assert
            Assertions.assertEquals(content, restored);
        }

        // Cleanup
        template.removeObject(bucket, objectKey);
        template.removeBucket(bucket);
    }
}
