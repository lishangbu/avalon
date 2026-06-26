package io.github.lishangbu.s3.autoconfigure

import io.github.lishangbu.s3.S3ClientSettings
import io.github.lishangbu.s3.S3ListObjectsCommand
import io.github.lishangbu.s3.S3ObjectKey
import io.github.lishangbu.s3.S3Operations
import io.github.lishangbu.s3.S3PresignGetObjectCommand
import io.github.lishangbu.s3.S3PresignPutObjectCommand
import io.github.lishangbu.s3.S3PutObjectCommand
import io.github.lishangbu.s3.S3PutObjectStreamCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.S3Exception
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * 验证 S3 自动配置的装配条件、配置校验和真实 S3 兼容服务访问能力。
 *
 * 集成用例通过 MinIO Testcontainers 覆盖 starter 暴露的主要操作，避免只验证 Bean
 * 存在而遗漏 SDK 配置和对象 key 前缀处理问题。
 */
@Testcontainers
class S3AutoConfigurationTests {
	private val contextRunner = ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(S3AutoConfiguration::class.java))

	@Test
	fun `auto configuration is disabled by default`() {
		contextRunner.run { context ->
			assertThat(context).doesNotHaveBean(S3Operations::class.java)
			assertThat(context).doesNotHaveBean(S3Client::class.java)
		}
	}

	@Test
	fun `enabled configuration creates S3 beans`() {
		contextRunner
			.withPropertyValues(*minioProperties())
			.run { context ->
				assertThat(context).hasSingleBean(S3Client::class.java)
				assertThat(context).hasSingleBean(S3Operations::class.java)
				assertThat(context).hasSingleBean(S3ClientSettings::class.java)
				assertThat(context.getBean(S3ClientSettings::class.java).defaultPresignTtl)
					.isEqualTo(Duration.ofMinutes(15))
			}
	}

	@Test
	fun `custom operations bean is preserved`() {
		contextRunner
			.withUserConfiguration(CustomOperationsConfiguration::class.java)
			.withPropertyValues(*minioProperties())
			.run { context ->
				assertThat(context).hasSingleBean(S3Operations::class.java)
				assertThat(context.getBean(S3Operations::class.java))
					.isInstanceOf(StubS3Operations::class.java)
			}
	}

	@Test
	fun `enabled configuration fails when bucket is blank`() {
		assertConfigurationFailure(
			"s3.bucket 不能为空",
			"s3.enabled=true",
			"s3.bucket= ",
		)
	}

	@Test
	fun `enabled configuration fails when access key is missing secret key`() {
		assertConfigurationFailure(
			"s3.credentials.access-key 和 secret-key 必须同时配置",
			*s3PropertiesWithoutCredentials(),
			"s3.credentials.access-key=access-only",
		)
	}

	@Test
	fun `enabled configuration fails when secret key is missing access key`() {
		assertConfigurationFailure(
			"s3.credentials.access-key 和 secret-key 必须同时配置",
			*s3PropertiesWithoutCredentials(),
			"s3.credentials.secret-key=secret-only",
		)
	}

	@Test
	fun `enabled configuration fails when default presign ttl is zero`() {
		assertConfigurationFailure(
			"s3.presign.default-ttl 必须大于 0",
			*minioProperties(),
			"s3.presign.default-ttl=0s",
		)
	}

	@Test
	fun `enabled configuration fails when default presign ttl is negative`() {
		assertConfigurationFailure(
			"s3.presign.default-ttl 必须大于 0",
			*minioProperties(),
			"s3.presign.default-ttl=-1s",
		)
	}

	@Test
	fun `enabled configuration can use default aws credential chain`() {
		contextRunner
			.withPropertyValues(*s3PropertiesWithoutCredentials())
			.run { context ->
				assertThat(context).hasSingleBean(S3Client::class.java)
				assertThat(context).hasSingleBean(S3Operations::class.java)
				assertThat(context).hasSingleBean(S3ClientSettings::class.java)
			}
	}

	@Test
	fun `operations can put get stream head list presign and delete object on MinIO`() {
		contextRunner
			.withPropertyValues(*minioProperties())
			.run { context ->
				val s3Client = context.getBean(S3Client::class.java)
				s3Client.ensureBucket(BUCKET)

				val operations = context.getBean(S3Operations::class.java)
				val key = S3ObjectKey.of("samples/hello.txt")
				val secondKey = S3ObjectKey.of("samples/second.txt")
				val streamKey = S3ObjectKey.of("samples/stream.txt")
				val streamContent = "stream-minio".toByteArray(StandardCharsets.UTF_8)
				operations.putObject(
					S3PutObjectCommand(
						key = key,
						content = "hello-minio".toByteArray(StandardCharsets.UTF_8),
						contentType = "text/plain",
						metadata = mapOf("purpose" to "integration-test"),
					),
				)
				operations.putObject(
					S3PutObjectCommand(
						key = secondKey,
						content = "second".toByteArray(StandardCharsets.UTF_8),
						contentType = "text/plain",
					),
				)
				operations.putObject(
					S3PutObjectStreamCommand(
						key = streamKey,
						content = ByteArrayInputStream(streamContent),
						contentLength = streamContent.size.toLong(),
						contentType = "text/plain",
						metadata = mapOf("mode" to "stream"),
					),
				)

				assertThat(operations.objectExists(key)).isTrue()
				val metadata = operations.headObject(key)
				assertThat(metadata.contentType).isEqualTo("text/plain")
				assertThat(metadata.metadata).containsEntry("purpose", "integration-test")
				assertThat(metadata.contentLength).isEqualTo("hello-minio".length.toLong())

				val page = operations.listObjects(
					S3ListObjectsCommand(prefix = S3ObjectKey.of("samples/"), maxKeys = 10),
				)
				assertThat(page.objects.map { it.key }).contains(key, secondKey, streamKey)
				assertThat(page.truncated).isFalse()
				assertThat(page.nextContinuationToken).isNull()

				val content = operations.getObject(key)
				assertThat(String(content.content, StandardCharsets.UTF_8)).isEqualTo("hello-minio")
				assertThat(content.contentType).isEqualTo("text/plain")
				assertThat(content.metadata).containsEntry("purpose", "integration-test")

				operations.getObjectStream(streamKey).use { streamObject ->
					assertThat(String(streamObject.content.readAllBytes(), StandardCharsets.UTF_8))
						.isEqualTo("stream-minio")
					assertThat(streamObject.contentType).isEqualTo("text/plain")
					assertThat(streamObject.metadata).containsEntry("mode", "stream")
					assertThat(streamObject.contentLength).isEqualTo(streamContent.size.toLong())
				}

				val presignedUrl = operations.createPresignedGetUrl(
					S3PresignGetObjectCommand(key, Duration.ofMinutes(5)),
				)
				assertThat(presignedUrl.url.scheme).startsWith("http")
				assertThat(presignedUrl.method).isEqualTo("GET")

				val defaultPresignedUrl = operations.createPresignedGetUrl(key)
				assertThat(defaultPresignedUrl.url.scheme).startsWith("http")
				assertThat(defaultPresignedUrl.method).isEqualTo("GET")

				operations.deleteObject(streamKey)
				operations.deleteObject(secondKey)
				operations.deleteObject(key)
				assertThat(operations.objectExists(key)).isFalse()
			}
	}

	@Test
	fun `list returns keys relative to configured key prefix`() {
		contextRunner
			.withPropertyValues(*minioProperties(), "s3.key-prefix=tenant-a")
			.run { context ->
				val s3Client = context.getBean(S3Client::class.java)
				s3Client.ensureBucket(BUCKET)

				val operations = context.getBean(S3Operations::class.java)
				val key = S3ObjectKey.of("reports/prefixed.txt")
				operations.putObject(
					S3PutObjectCommand(
						key = key,
						content = "prefixed".toByteArray(StandardCharsets.UTF_8),
						contentType = "text/plain",
					),
				)

				val page = operations.listObjects(
					S3ListObjectsCommand(prefix = S3ObjectKey.of("reports/"), maxKeys = 10),
				)
				assertThat(page.objects.map { it.key }).contains(key)
				assertThat(page.objects.map { it.key.value }).doesNotContain("tenant-a/reports/prefixed.txt")
				assertThat(operations.listObjects().objects.map { it.key }).contains(key)

				operations.deleteObject(key)
			}
	}

	/**
	 * 将自动配置指向当前测试类启动的 MinIO 实例，确保 SDK 使用可重复的本地 S3 兼容端点。
	 */
	private fun minioProperties(): Array<String> =
		arrayOf(
			"s3.enabled=true",
			"s3.bucket=$BUCKET",
			"s3.region=us-east-1",
			"s3.endpoint=http://${minio.host}:${minio.getMappedPort(MINIO_PORT)}",
			"s3.path-style-access-enabled=true",
			"s3.credentials.access-key=$MINIO_ACCESS_KEY",
			"s3.credentials.secret-key=$MINIO_SECRET_KEY",
		)

	/**
	 * 保留最小 S3 配置，用于验证未显式配置 AK/SK 时仍会装配默认 AWS 凭证链。
	 */
	private fun s3PropertiesWithoutCredentials(): Array<String> =
		arrayOf(
			"s3.enabled=true",
			"s3.bucket=$BUCKET",
			"s3.region=us-east-1",
		)

	/**
	 * 统一断言自动配置启动失败的根因，避免校验类异常被 Spring 包装后误判为普通启动错误。
	 */
	private fun assertConfigurationFailure(message: String, vararg properties: String) {
		contextRunner
			.withPropertyValues(*properties)
			.run { context ->
				assertThat(context).hasFailed()
				assertThat(context.startupFailure)
					.hasRootCauseInstanceOf(S3ConfigurationException::class.java)
					.hasMessageContaining(message)
			}
	}

	/**
	 * 幂等创建测试 bucket；同一个 MinIO 容器在类内复用时，重复创建只接受 409 冲突。
	 */
	private fun S3Client.ensureBucket(bucket: String) {
		try {
			createBucket { builder ->
				builder.bucket(bucket)
			}
		} catch (ex: S3Exception) {
			if (ex.statusCode() != CONFLICT_STATUS) {
				throw ex
			}
		}
	}

	/**
	 * 提供用户侧自定义 [S3Operations]，用于验证 starter 不会覆盖业务已经声明的 Bean。
	 */
	@Configuration(proxyBeanMethods = false)
	private class CustomOperationsConfiguration {
		@Bean
		fun stubS3Operations(): S3Operations =
			StubS3Operations()
	}

	/**
	 * 自定义操作 Bean 的占位实现；测试只关心 Bean 选择结果，因此所有真实操作都应不可达。
	 */
	private class StubS3Operations : S3Operations {
		override fun putObject(command: S3PutObjectCommand) =
			throw UnsupportedOperationException()

		override fun putObject(command: S3PutObjectStreamCommand) =
			throw UnsupportedOperationException()

		override fun getObject(key: S3ObjectKey) =
			throw UnsupportedOperationException()

		override fun getObjectStream(key: S3ObjectKey) =
			throw UnsupportedOperationException()

		override fun headObject(key: S3ObjectKey) =
			throw UnsupportedOperationException()

		override fun deleteObject(key: S3ObjectKey) {
			throw UnsupportedOperationException()
		}

		override fun objectExists(key: S3ObjectKey) =
			false

		override fun listObjects(command: S3ListObjectsCommand) =
			throw UnsupportedOperationException()

		override fun listObjects() =
			throw UnsupportedOperationException()

		override fun createPresignedGetUrl(command: S3PresignGetObjectCommand) =
			throw UnsupportedOperationException()

		override fun createPresignedGetUrl(key: S3ObjectKey) =
			throw UnsupportedOperationException()

		override fun createPresignedPutUrl(
			command: S3PresignPutObjectCommand,
		) = throw UnsupportedOperationException()

		override fun createPresignedPutUrl(key: S3ObjectKey) =
			throw UnsupportedOperationException()
	}

	private companion object {
		private const val BUCKET = "s3-starter-test"
		private const val MINIO_PORT = 9000
		private const val MINIO_ACCESS_KEY = "minioadmin"
		private const val MINIO_SECRET_KEY = "minioadmin"
		private const val CONFLICT_STATUS = 409

		/**
		 * 手动启动 MinIO，使动态端口在构造 ApplicationContextRunner 属性前已经可用。
		 */
		private val minio: GenericContainer<*> = GenericContainer(DockerImageName.parse("minio/minio:latest"))
			.withEnv("MINIO_ROOT_USER", MINIO_ACCESS_KEY)
			.withEnv("MINIO_ROOT_PASSWORD", MINIO_SECRET_KEY)
			.withExposedPorts(MINIO_PORT)
			.withCommand("server /data")
			.waitingFor(Wait.forHttp("/minio/health/ready").forPort(MINIO_PORT))

		init {
			minio.start()
		}
	}
}
