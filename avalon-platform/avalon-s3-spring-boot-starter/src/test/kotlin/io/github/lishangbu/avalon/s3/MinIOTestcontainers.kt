package io.github.lishangbu.avalon.s3

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.MinIOContainer
import org.testcontainers.utility.DockerImageName

/**
 * MinIO 测试容器扩展
 *
 * 为 S3 相关测试启动并暴露 MinIO 容器
 */
class MinIOTestcontainers : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext) {
        container.start()
        val mappedPort = container.firstMappedPort
        val url = "http://${container.host}:$mappedPort"
        System.setProperty("avalon.s3.enabled", "true")
        System.setProperty("avalon.s3.default-client-name", "default")
        System.setProperty("avalon.s3.clients.default.provider", "MINIO")
        System.setProperty("avalon.s3.clients.default.endpoint", url)
        System.setProperty("avalon.s3.clients.default.region", "us-east-1")
        System.setProperty("avalon.s3.clients.default.path-style-access", "true")
        System.setProperty("avalon.s3.clients.default.credentials.access-key-id", "testuser")
        System.setProperty("avalon.s3.clients.default.credentials.secret-access-key", "testpassword")
    }

    /** 暴露运行中的 MinIO 容器 */
    @Bean
    @ServiceConnection
    fun minIOContainer(): MinIOContainer = container

    companion object {
        private val container: MinIOContainer =
            MinIOContainer(DockerImageName.parse("minio/minio:latest"))
                .withUserName("testuser")
                .withPassword("testpassword")
                .withReuse(true)
    }
}
