package io.github.lishangbu.avalon.s3.client

import io.github.lishangbu.avalon.s3.facade.S3Facade
import io.github.lishangbu.avalon.s3.properties.S3Provider
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.waiters.S3Waiter
import software.amazon.awssdk.services.s3control.S3ControlAsyncClient
import software.amazon.awssdk.services.s3control.S3ControlClient
import software.amazon.awssdk.transfer.s3.S3TransferManager

/** 命名 client 的完整能力集合。 */
class AvalonS3ClientBundle(
    val name: String,
    val provider: S3Provider,
    val bucketAliases: Map<String, String>,
    val s3Client: S3Client,
    val s3AsyncClient: S3AsyncClient,
    val s3Waiter: S3Waiter,
    val s3Presigner: S3Presigner,
    val s3ControlClient: S3ControlClient,
    val s3ControlAsyncClient: S3ControlAsyncClient,
    val s3TransferManager: S3TransferManager,
) {
    /** 聚合 facade。 */
    val facade: S3Facade =
        S3Facade(
            clientName = name,
            provider = provider,
            bucketAliases = bucketAliases,
            s3Client = s3Client,
            s3AsyncClient = s3AsyncClient,
            s3Waiter = s3Waiter,
            s3Presigner = s3Presigner,
            s3ControlClient = s3ControlClient,
            s3ControlAsyncClient = s3ControlAsyncClient,
            s3TransferManager = s3TransferManager,
        )
}
