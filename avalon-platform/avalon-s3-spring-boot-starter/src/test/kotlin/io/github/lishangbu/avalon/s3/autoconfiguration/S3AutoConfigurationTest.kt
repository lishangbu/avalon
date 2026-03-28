package io.github.lishangbu.avalon.s3.autoconfiguration

import io.github.lishangbu.avalon.s3.client.AvalonS3ClientRegistry
import io.github.lishangbu.avalon.s3.facade.S3Facade
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3control.S3ControlClient
import software.amazon.awssdk.transfer.s3.S3TransferManager
import java.time.Duration

/** 验证 Avalon S3 自动配置。 */
class S3AutoConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner().withConfiguration(
            AutoConfigurations.of(S3AutoConfiguration::class.java),
        )

    @Test
    fun shouldExposeDefaultClientsAndFacade() {
        contextRunner
            .withPropertyValues(
                "avalon.s3.enabled=true",
                "avalon.s3.clients.default.provider=MINIO",
                "avalon.s3.clients.default.endpoint=http://localhost:9000",
                "avalon.s3.clients.default.region=us-east-1",
                "avalon.s3.clients.default.path-style-access=true",
                "avalon.s3.clients.default.credentials.access-key-id=testuser",
                "avalon.s3.clients.default.credentials.secret-access-key=testpassword",
            ).run { context ->
                assertThat(context).hasSingleBean(AvalonS3ClientRegistry::class.java)
                assertThat(context).hasSingleBean(S3Facade::class.java)
                assertThat(context).hasSingleBean(S3Client::class.java)
                assertThat(context).hasSingleBean(S3AsyncClient::class.java)
                assertThat(context).hasSingleBean(S3Presigner::class.java)
                assertThat(context).hasSingleBean(S3ControlClient::class.java)
                assertThat(context).hasSingleBean(S3TransferManager::class.java)
            }
    }

    @Test
    fun shouldKeepMultipleNamedClientsInRegistry() {
        contextRunner
            .withPropertyValues(
                "avalon.s3.enabled=true",
                "avalon.s3.default-client-name=archive",
                "avalon.s3.clients.default.provider=MINIO",
                "avalon.s3.clients.default.endpoint=http://localhost:9000",
                "avalon.s3.clients.default.region=us-east-1",
                "avalon.s3.clients.default.path-style-access=true",
                "avalon.s3.clients.default.credentials.access-key-id=testuser",
                "avalon.s3.clients.default.credentials.secret-access-key=testpassword",
                "avalon.s3.clients.archive.provider=MINIO",
                "avalon.s3.clients.archive.endpoint=http://localhost:9001",
                "avalon.s3.clients.archive.region=us-east-1",
                "avalon.s3.clients.archive.path-style-access=true",
                "avalon.s3.clients.archive.credentials.access-key-id=testuser",
                "avalon.s3.clients.archive.credentials.secret-access-key=testpassword",
                "avalon.s3.clients.archive.bucket-aliases.media=real-media-bucket",
            ).run { context ->
                val registry = context.getBean(AvalonS3ClientRegistry::class.java)
                assertThat(registry.clientNames()).containsExactlyInAnyOrder("default", "archive")
                assertThat(registry.defaultFacade().clientName).isEqualTo("archive")
                assertThat(registry.facade("archive").resolveBucketName("media")).isEqualTo("real-media-bucket")
            }
    }

    @Test
    fun shouldUsePathStyleForRustFsByDefault() {
        contextRunner
            .withPropertyValues(
                "avalon.s3.enabled=true",
                "avalon.s3.clients.default.provider=RUSTFS",
                "avalon.s3.clients.default.endpoint=https://rustfs.example.com",
                "avalon.s3.clients.default.credentials.access-key-id=testuser",
                "avalon.s3.clients.default.credentials.secret-access-key=testpassword",
            ).run { context ->
                val s3Facade = context.getBean(S3Facade::class.java)
                val url =
                    s3Facade.presign
                        .get("demo-bucket", "hello.txt", Duration.ofMinutes(5))
                        .url()
                        .toString()

                assertThat(url).startsWith("https://rustfs.example.com/demo-bucket/hello.txt")
            }
    }

    @Test
    fun shouldUseVirtualHostedStyleForAliyunOssByDefault() {
        contextRunner
            .withPropertyValues(
                "avalon.s3.enabled=true",
                "avalon.s3.clients.default.provider=ALIYUN_OSS",
                "avalon.s3.clients.default.endpoint=https://oss-cn-hangzhou.aliyuncs.com",
                "avalon.s3.clients.default.region=cn-hangzhou",
                "avalon.s3.clients.default.credentials.access-key-id=testuser",
                "avalon.s3.clients.default.credentials.secret-access-key=testpassword",
            ).run { context ->
                val s3Facade = context.getBean(S3Facade::class.java)
                val url =
                    s3Facade.presign
                        .get("demo-bucket", "hello.txt", Duration.ofMinutes(5))
                        .url()
                        .toString()

                assertThat(url).startsWith("https://demo-bucket.oss-cn-hangzhou.aliyuncs.com/hello.txt")
            }
    }

    @Test
    fun shouldUseVirtualHostedStyleForQiniuKodoByDefault() {
        contextRunner
            .withPropertyValues(
                "avalon.s3.enabled=true",
                "avalon.s3.clients.default.provider=QINIU_KODO",
                "avalon.s3.clients.default.endpoint=https://s3.cn-east-1.qiniucs.com",
                "avalon.s3.clients.default.region=cn-east-1",
                "avalon.s3.clients.default.credentials.access-key-id=testuser",
                "avalon.s3.clients.default.credentials.secret-access-key=testpassword",
            ).run { context ->
                val s3Facade = context.getBean(S3Facade::class.java)
                val url =
                    s3Facade.presign
                        .get("demo-bucket", "hello.txt", Duration.ofMinutes(5))
                        .url()
                        .toString()

                assertThat(url).startsWith("https://demo-bucket.s3.cn-east-1.qiniucs.com/hello.txt")
            }
    }

    @Test
    fun shouldFailWhenAliyunOssRegionMissing() {
        contextRunner
            .withPropertyValues(
                "avalon.s3.enabled=true",
                "avalon.s3.clients.default.provider=ALIYUN_OSS",
                "avalon.s3.clients.default.endpoint=https://oss-cn-hangzhou.aliyuncs.com",
                "avalon.s3.clients.default.credentials.access-key-id=testuser",
                "avalon.s3.clients.default.credentials.secret-access-key=testpassword",
            ).run { context ->
                assertThat(context).hasFailed()
                assertThat(context)
                    .getFailure()
                    .hasMessageContaining("requires 'region'")
                    .hasMessageContaining("ALIYUN_OSS")
            }
    }

    @Test
    fun shouldFailWhenRustFsEndpointMissing() {
        contextRunner
            .withPropertyValues(
                "avalon.s3.enabled=true",
                "avalon.s3.clients.default.provider=RUSTFS",
                "avalon.s3.clients.default.credentials.access-key-id=testuser",
                "avalon.s3.clients.default.credentials.secret-access-key=testpassword",
            ).run { context ->
                assertThat(context).hasFailed()
                assertThat(context)
                    .getFailure()
                    .hasMessageContaining("requires 'endpoint'")
                    .hasMessageContaining("RUSTFS")
            }
    }
}
