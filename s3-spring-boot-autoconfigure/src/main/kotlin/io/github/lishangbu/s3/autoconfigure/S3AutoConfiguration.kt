package io.github.lishangbu.s3.autoconfigure

import io.github.lishangbu.s3.S3ClientSettings
import io.github.lishangbu.s3.DefaultS3Operations
import io.github.lishangbu.s3.S3Operations
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner

/**
 * S3 starter 的自动配置入口。
 */
@AutoConfiguration
@ConditionalOnClass(S3Client::class)
@EnableConfigurationProperties(S3Properties::class)
@ConditionalOnProperty(prefix = "s3", name = ["enabled"], havingValue = "true")
class S3AutoConfiguration {
	@Bean
	@ConditionalOnMissingBean
	fun s3ClientSettings(properties: S3Properties): S3ClientSettings =
		S3ClientSettings(
			bucket = properties.requiredBucket(),
			keyPrefix = properties.keyPrefix.trim('/'),
			defaultPresignTtl = properties.requiredDefaultPresignTtl(),
		)

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean
	fun s3Client(properties: S3Properties): S3Client {
		val builder = S3Client.builder()
			.region(properties.region())
			.credentialsProvider(properties.credentialsProvider())
			.serviceConfiguration(properties.serviceConfiguration())
		properties.endpoint?.let(builder::endpointOverride)
		return builder.build()
	}

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean
	fun s3Presigner(properties: S3Properties): S3Presigner {
		val builder = S3Presigner.builder()
			.region(properties.region())
			.credentialsProvider(properties.credentialsProvider())
			.serviceConfiguration(properties.serviceConfiguration())
		properties.endpoint?.let(builder::endpointOverride)
		return builder.build()
	}

	@Bean
	@ConditionalOnMissingBean
	fun s3Operations(
		s3Client: S3Client,
		s3Presigner: S3Presigner,
		settings: S3ClientSettings,
	): S3Operations =
		DefaultS3Operations(s3Client, s3Presigner, settings)

	private fun S3Properties.requiredBucket(): String =
		bucket.trim().ifBlank {
			throw S3ConfigurationException("s3.bucket 不能为空")
		}

	private fun S3Properties.requiredDefaultPresignTtl() =
		presign.defaultTtl.takeIf { !it.isZero && !it.isNegative }
			?: throw S3ConfigurationException("s3.presign.default-ttl 必须大于 0")

	private fun S3Properties.region(): Region =
		Region.of(region.trim().ifBlank { DEFAULT_REGION })

	private fun S3Properties.serviceConfiguration(): S3Configuration =
		S3Configuration.builder()
			.pathStyleAccessEnabled(pathStyleAccessEnabled)
			.build()

	private fun S3Properties.credentialsProvider(): AwsCredentialsProvider {
		val accessKey = credentials.accessKey.trim()
		val secretKey = credentials.secretKey.trim()
		if (accessKey.isBlank() && secretKey.isBlank()) {
			return DefaultCredentialsProvider.builder().build()
		}
		if (accessKey.isBlank() || secretKey.isBlank()) {
			throw S3ConfigurationException("s3.credentials.access-key 和 secret-key 必须同时配置")
		}
		val sessionToken = credentials.sessionToken.trim()
		val credentialsValue = if (sessionToken.isBlank()) {
			AwsBasicCredentials.create(accessKey, secretKey)
		} else {
			AwsSessionCredentials.create(accessKey, secretKey, sessionToken)
		}
		return StaticCredentialsProvider.create(credentialsValue)
	}

	private companion object {
		private const val DEFAULT_REGION = "us-east-1"
	}
}
