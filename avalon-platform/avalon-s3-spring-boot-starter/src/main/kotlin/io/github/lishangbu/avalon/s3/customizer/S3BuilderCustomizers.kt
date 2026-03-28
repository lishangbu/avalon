package io.github.lishangbu.avalon.s3.customizer

import io.github.lishangbu.avalon.s3.properties.S3ClientProperties
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder
import software.amazon.awssdk.services.s3.S3ClientBuilder
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3control.S3ControlAsyncClientBuilder
import software.amazon.awssdk.services.s3control.S3ControlClientBuilder
import software.amazon.awssdk.transfer.s3.S3TransferManager

/** 自定义同步 S3 client builder。 */
fun interface S3ClientBuilderCustomizer {
    fun customize(
        clientName: String,
        properties: S3ClientProperties,
        builder: S3ClientBuilder,
    )
}

/** 自定义异步 S3 client builder。 */
fun interface S3AsyncClientBuilderCustomizer {
    fun customize(
        clientName: String,
        properties: S3ClientProperties,
        builder: S3AsyncClientBuilder,
    )
}

/** 自定义 S3 presigner builder。 */
fun interface S3PresignerBuilderCustomizer {
    fun customize(
        clientName: String,
        properties: S3ClientProperties,
        builder: S3Presigner.Builder,
    )
}

/** 自定义同步 S3 Control client builder。 */
fun interface S3ControlClientBuilderCustomizer {
    fun customize(
        clientName: String,
        properties: S3ClientProperties,
        builder: S3ControlClientBuilder,
    )
}

/** 自定义异步 S3 Control client builder。 */
fun interface S3ControlAsyncClientBuilderCustomizer {
    fun customize(
        clientName: String,
        properties: S3ClientProperties,
        builder: S3ControlAsyncClientBuilder,
    )
}

/** 自定义 Transfer Manager builder。 */
fun interface S3TransferManagerBuilderCustomizer {
    fun customize(
        clientName: String,
        properties: S3ClientProperties,
        builder: S3TransferManager.Builder,
    )
}
