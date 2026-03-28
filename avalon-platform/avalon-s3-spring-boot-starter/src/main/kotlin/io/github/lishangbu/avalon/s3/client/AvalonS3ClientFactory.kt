package io.github.lishangbu.avalon.s3.client

import io.github.lishangbu.avalon.s3.customizer.S3AsyncClientBuilderCustomizer
import io.github.lishangbu.avalon.s3.customizer.S3ClientBuilderCustomizer
import io.github.lishangbu.avalon.s3.customizer.S3ControlAsyncClientBuilderCustomizer
import io.github.lishangbu.avalon.s3.customizer.S3ControlClientBuilderCustomizer
import io.github.lishangbu.avalon.s3.customizer.S3PresignerBuilderCustomizer
import io.github.lishangbu.avalon.s3.customizer.S3TransferManagerBuilderCustomizer
import io.github.lishangbu.avalon.s3.properties.S3ClientOverrideProperties
import io.github.lishangbu.avalon.s3.properties.S3ClientProperties
import io.github.lishangbu.avalon.s3.properties.S3CredentialsProperties
import io.github.lishangbu.avalon.s3.properties.S3HttpClientProperties
import io.github.lishangbu.avalon.s3.properties.S3Provider
import org.springframework.util.StringUtils
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3control.S3ControlAsyncClient
import software.amazon.awssdk.services.s3control.S3ControlClient
import software.amazon.awssdk.transfer.s3.S3TransferManager
import java.net.URI

/** 负责根据配置实例化命名 S3 client。 */
class AvalonS3ClientFactory(
    private val s3ClientBuilderCustomizers: List<S3ClientBuilderCustomizer>,
    private val s3AsyncClientBuilderCustomizers: List<S3AsyncClientBuilderCustomizer>,
    private val s3PresignerBuilderCustomizers: List<S3PresignerBuilderCustomizer>,
    private val s3ControlClientBuilderCustomizers: List<S3ControlClientBuilderCustomizer>,
    private val s3ControlAsyncClientBuilderCustomizers: List<S3ControlAsyncClientBuilderCustomizer>,
    private val s3TransferManagerBuilderCustomizers: List<S3TransferManagerBuilderCustomizer>,
) {
    /** 创建一个命名 bundle。 */
    fun create(
        clientName: String,
        properties: S3ClientProperties,
    ): AvalonS3ClientBundle {
        validateProperties(clientName, properties)

        val credentialsProvider = credentialsProvider(properties.credentials)
        val region = resolveRegion(properties)
        val endpoint = properties.endpoint?.takeIf(StringUtils::hasText)?.let(URI::create)

        val s3ClientBuilder =
            S3Client
                .builder()
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(serviceConfiguration(properties))
                .overrideConfiguration(overrideConfiguration(properties.overrides))
                .httpClient(syncHttpClient(properties.http))
        if (region != null) {
            s3ClientBuilder.region(region)
        }
        if (endpoint != null) {
            s3ClientBuilder.endpointOverride(endpoint)
        }
        s3ClientBuilderCustomizers.forEach { it.customize(clientName, properties, s3ClientBuilder) }
        val s3Client = s3ClientBuilder.build()

        val s3AsyncClientBuilder =
            S3AsyncClient
                .builder()
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(serviceConfiguration(properties))
                .overrideConfiguration(overrideConfiguration(properties.overrides))
                .multipartEnabled(properties.transfer.multipartEnabled)
                .httpClient(asyncHttpClient(properties.http))
        if (region != null) {
            s3AsyncClientBuilder.region(region)
        }
        if (endpoint != null) {
            s3AsyncClientBuilder.endpointOverride(endpoint)
        }
        s3AsyncClientBuilderCustomizers.forEach { it.customize(clientName, properties, s3AsyncClientBuilder) }
        val s3AsyncClient = s3AsyncClientBuilder.build()

        val s3PresignerBuilder =
            S3Presigner
                .builder()
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(serviceConfiguration(properties))
        if (region != null) {
            s3PresignerBuilder.region(region)
        }
        if (endpoint != null) {
            s3PresignerBuilder.endpointOverride(endpoint)
        }
        s3PresignerBuilderCustomizers.forEach { it.customize(clientName, properties, s3PresignerBuilder) }
        val s3Presigner = s3PresignerBuilder.build()

        val s3ControlClientBuilder =
            S3ControlClient
                .builder()
                .credentialsProvider(credentialsProvider)
                .overrideConfiguration(overrideConfiguration(properties.overrides))
                .httpClient(syncHttpClient(properties.http))
        if (region != null) {
            s3ControlClientBuilder.region(region)
        }
        if (endpoint != null && properties.provider != S3Provider.AWS) {
            s3ControlClientBuilder.endpointOverride(endpoint)
        }
        s3ControlClientBuilderCustomizers.forEach { it.customize(clientName, properties, s3ControlClientBuilder) }
        val s3ControlClient = s3ControlClientBuilder.build()

        val s3ControlAsyncClientBuilder =
            S3ControlAsyncClient
                .builder()
                .credentialsProvider(credentialsProvider)
                .overrideConfiguration(overrideConfiguration(properties.overrides))
                .httpClient(asyncHttpClient(properties.http))
        if (region != null) {
            s3ControlAsyncClientBuilder.region(region)
        }
        if (endpoint != null && properties.provider != S3Provider.AWS) {
            s3ControlAsyncClientBuilder.endpointOverride(endpoint)
        }
        s3ControlAsyncClientBuilderCustomizers.forEach { it.customize(clientName, properties, s3ControlAsyncClientBuilder) }
        val s3ControlAsyncClient = s3ControlAsyncClientBuilder.build()

        val s3TransferManagerBuilder = S3TransferManager.builder().s3Client(s3AsyncClient)
        s3TransferManagerBuilderCustomizers.forEach { it.customize(clientName, properties, s3TransferManagerBuilder) }
        val s3TransferManager = s3TransferManagerBuilder.build()

        return AvalonS3ClientBundle(
            name = clientName,
            provider = properties.provider,
            bucketAliases = properties.bucketAliases.toMap(),
            s3Client = s3Client,
            s3AsyncClient = s3AsyncClient,
            s3Waiter = s3Client.waiter(),
            s3Presigner = s3Presigner,
            s3ControlClient = s3ControlClient,
            s3ControlAsyncClient = s3ControlAsyncClient,
            s3TransferManager = s3TransferManager,
        )
    }

    private fun syncHttpClient(properties: S3HttpClientProperties): SdkHttpClient {
        val builder = ApacheHttpClient.builder()
        properties.connectionTimeout?.let(builder::connectionTimeout)
        properties.socketTimeout?.let(builder::socketTimeout)
        properties.maxConnections?.let(builder::maxConnections)
        return builder.build()
    }

    private fun asyncHttpClient(properties: S3HttpClientProperties): SdkAsyncHttpClient {
        val builder = NettyNioAsyncHttpClient.builder()
        properties.connectionTimeout?.let(builder::connectionTimeout)
        properties.readTimeout?.let(builder::readTimeout)
        properties.writeTimeout?.let(builder::writeTimeout)
        properties.maxConcurrency?.let(builder::maxConcurrency)
        return builder.build()
    }

    @Suppress("DEPRECATION")
    private fun serviceConfiguration(properties: S3ClientProperties): S3Configuration {
        val builder = S3Configuration.builder()
        builder.pathStyleAccessEnabled(resolvePathStyleAccess(properties))
        builder.useArnRegionEnabled(properties.useArnRegionEnabled)
        builder.dualstackEnabled(properties.dualstackEnabled)
        builder.accelerateModeEnabled(properties.accelerateModeEnabled)
        properties.chunkedEncodingEnabled?.let(builder::chunkedEncodingEnabled)
        properties.checksumValidationEnabled?.let(builder::checksumValidationEnabled)
        return builder.build()
    }

    private fun resolvePathStyleAccess(properties: S3ClientProperties): Boolean =
        properties.pathStyleAccess ?: when (properties.provider) {
            S3Provider.AWS -> false
            S3Provider.MINIO, S3Provider.RUSTFS, S3Provider.GENERIC_S3 -> true
            S3Provider.ALIYUN_OSS, S3Provider.QINIU_KODO -> false
        }

    private fun overrideConfiguration(properties: S3ClientOverrideProperties): ClientOverrideConfiguration {
        val builder = ClientOverrideConfiguration.builder()
        properties.apiCallTimeout?.let(builder::apiCallTimeout)
        properties.apiCallAttemptTimeout?.let(builder::apiCallAttemptTimeout)
        return builder.build()
    }

    private fun resolveRegion(properties: S3ClientProperties): Region? {
        val configured = properties.region?.takeIf(StringUtils::hasText)
        if (configured != null) {
            return Region.of(configured)
        }
        return when (properties.provider) {
            S3Provider.AWS -> null
            S3Provider.MINIO, S3Provider.RUSTFS, S3Provider.GENERIC_S3 -> Region.US_EAST_1
            S3Provider.ALIYUN_OSS, S3Provider.QINIU_KODO -> null
        }
    }

    private fun validateProperties(
        clientName: String,
        properties: S3ClientProperties,
    ) {
        val endpointRequired =
            when (properties.provider) {
                S3Provider.AWS -> false

                S3Provider.MINIO,
                S3Provider.RUSTFS,
                S3Provider.ALIYUN_OSS,
                S3Provider.QINIU_KODO,
                S3Provider.GENERIC_S3,
                -> true
            }
        if (endpointRequired && !StringUtils.hasText(properties.endpoint)) {
            throw IllegalStateException(
                "S3 client '$clientName' requires 'endpoint' when provider is ${properties.provider}.",
            )
        }

        val regionRequired =
            when (properties.provider) {
                S3Provider.ALIYUN_OSS, S3Provider.QINIU_KODO -> true
                S3Provider.AWS, S3Provider.MINIO, S3Provider.RUSTFS, S3Provider.GENERIC_S3 -> false
            }
        if (regionRequired && !StringUtils.hasText(properties.region)) {
            throw IllegalStateException(
                "S3 client '$clientName' requires 'region' when provider is ${properties.provider}.",
            )
        }
    }

    private fun credentialsProvider(credentials: S3CredentialsProperties): AwsCredentialsProvider {
        val accessKeyId = credentials.accessKeyId?.takeIf(StringUtils::hasText)
        val secretAccessKey = credentials.secretAccessKey?.takeIf(StringUtils::hasText)
        val sessionToken = credentials.sessionToken?.takeIf(StringUtils::hasText)

        return when {
            accessKeyId != null && secretAccessKey != null && sessionToken != null -> {
                StaticCredentialsProvider.create(
                    AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken),
                )
            }

            accessKeyId != null && secretAccessKey != null -> {
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey),
                )
            }

            else -> {
                DefaultCredentialsProvider.builder().build()
            }
        }
    }
}
