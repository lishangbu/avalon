package io.github.lishangbu.avalon.s3.autoconfiguration

import io.github.lishangbu.avalon.s3.client.AvalonS3ClientFactory
import io.github.lishangbu.avalon.s3.client.AvalonS3ClientRegistry
import io.github.lishangbu.avalon.s3.customizer.S3AsyncClientBuilderCustomizer
import io.github.lishangbu.avalon.s3.customizer.S3ClientBuilderCustomizer
import io.github.lishangbu.avalon.s3.customizer.S3ControlAsyncClientBuilderCustomizer
import io.github.lishangbu.avalon.s3.customizer.S3ControlClientBuilderCustomizer
import io.github.lishangbu.avalon.s3.customizer.S3PresignerBuilderCustomizer
import io.github.lishangbu.avalon.s3.customizer.S3TransferManagerBuilderCustomizer
import io.github.lishangbu.avalon.s3.facade.BucketOperations
import io.github.lishangbu.avalon.s3.facade.MultipartOperations
import io.github.lishangbu.avalon.s3.facade.ObjectOperations
import io.github.lishangbu.avalon.s3.facade.PresignOperations
import io.github.lishangbu.avalon.s3.facade.S3Facade
import io.github.lishangbu.avalon.s3.facade.TransferOperations
import io.github.lishangbu.avalon.s3.properties.AvalonS3Properties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.waiters.S3Waiter
import software.amazon.awssdk.services.s3control.S3ControlAsyncClient
import software.amazon.awssdk.services.s3control.S3ControlClient
import software.amazon.awssdk.transfer.s3.S3TransferManager

@AutoConfiguration
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(S3Client::class)
@ConditionalOnProperty(prefix = AvalonS3Properties.PREFIX, name = ["enabled"], havingValue = "true")
@EnableConfigurationProperties(AvalonS3Properties::class)
class S3AutoConfiguration {
    /** 创建 registry。 */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(AvalonS3ClientRegistry::class)
    fun avalonS3ClientRegistry(
        properties: AvalonS3Properties,
        s3ClientBuilderCustomizers: List<S3ClientBuilderCustomizer>,
        s3AsyncClientBuilderCustomizers: List<S3AsyncClientBuilderCustomizer>,
        s3PresignerBuilderCustomizers: List<S3PresignerBuilderCustomizer>,
        s3ControlClientBuilderCustomizers: List<S3ControlClientBuilderCustomizer>,
        s3ControlAsyncClientBuilderCustomizers: List<S3ControlAsyncClientBuilderCustomizer>,
        s3TransferManagerBuilderCustomizers: List<S3TransferManagerBuilderCustomizer>,
    ): AvalonS3ClientRegistry {
        val factory =
            AvalonS3ClientFactory(
                s3ClientBuilderCustomizers = s3ClientBuilderCustomizers,
                s3AsyncClientBuilderCustomizers = s3AsyncClientBuilderCustomizers,
                s3PresignerBuilderCustomizers = s3PresignerBuilderCustomizers,
                s3ControlClientBuilderCustomizers = s3ControlClientBuilderCustomizers,
                s3ControlAsyncClientBuilderCustomizers = s3ControlAsyncClientBuilderCustomizers,
                s3TransferManagerBuilderCustomizers = s3TransferManagerBuilderCustomizers,
            )

        val enabledClients =
            properties.clients
                .filterValues { it.enabled }
                .mapValues { (clientName, clientProperties) ->
                    factory.create(clientName, clientProperties)
                }

        return AvalonS3ClientRegistry(properties.defaultClientName, enabledClients)
    }

    /** 默认 facade。 */
    @Bean
    @ConditionalOnMissingBean
    fun s3Facade(registry: AvalonS3ClientRegistry): S3Facade = registry.defaultFacade()

    /** 默认同步 client。 */
    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(S3Client::class)
    fun s3Client(s3Facade: S3Facade): S3Client = s3Facade.s3Client

    /** 默认异步 client。 */
    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(S3AsyncClient::class)
    fun s3AsyncClient(s3Facade: S3Facade): S3AsyncClient = s3Facade.s3AsyncClient

    /** 默认 waiter。 */
    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(S3Waiter::class)
    fun s3Waiter(s3Facade: S3Facade): S3Waiter = s3Facade.s3Waiter

    /** 默认 presigner。 */
    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(S3Presigner::class)
    fun s3Presigner(s3Facade: S3Facade): S3Presigner = s3Facade.s3Presigner

    /** 默认同步 S3 Control client。 */
    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(S3ControlClient::class)
    fun s3ControlClient(s3Facade: S3Facade): S3ControlClient = s3Facade.s3ControlClient

    /** 默认异步 S3 Control client。 */
    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(S3ControlAsyncClient::class)
    fun s3ControlAsyncClient(s3Facade: S3Facade): S3ControlAsyncClient = s3Facade.s3ControlAsyncClient

    /** 默认 Transfer Manager。 */
    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(S3TransferManager::class)
    fun s3TransferManager(s3Facade: S3Facade): S3TransferManager = s3Facade.s3TransferManager

    /** 默认桶操作 facade。 */
    @Bean
    @ConditionalOnMissingBean
    fun bucketOperations(s3Facade: S3Facade): BucketOperations = s3Facade.buckets

    /** 默认对象操作 facade。 */
    @Bean
    @ConditionalOnMissingBean
    fun objectOperations(s3Facade: S3Facade): ObjectOperations = s3Facade.objects

    /** 默认 multipart facade。 */
    @Bean
    @ConditionalOnMissingBean
    fun multipartOperations(s3Facade: S3Facade): MultipartOperations = s3Facade.multipart

    /** 默认 presign facade。 */
    @Bean
    @ConditionalOnMissingBean
    fun presignOperations(s3Facade: S3Facade): PresignOperations = s3Facade.presign

    /** 默认 transfer facade。 */
    @Bean
    @ConditionalOnMissingBean
    fun transferOperations(s3Facade: S3Facade): TransferOperations = s3Facade.transfer
}
