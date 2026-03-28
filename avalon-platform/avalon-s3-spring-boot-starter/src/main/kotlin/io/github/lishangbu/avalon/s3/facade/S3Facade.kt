package io.github.lishangbu.avalon.s3.facade

import io.github.lishangbu.avalon.s3.properties.S3Provider
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.waiters.S3Waiter
import software.amazon.awssdk.services.s3control.S3ControlAsyncClient
import software.amazon.awssdk.services.s3control.S3ControlClient
import software.amazon.awssdk.transfer.s3.S3TransferManager

/**
 * 一个命名 S3 client 的能力聚合视图。
 *
 * 所有 AWS 原生能力都可以通过原生 client 访问，常用数据面操作通过 facade 访问。
 */
class S3Facade(
    val clientName: String,
    val provider: S3Provider,
    private val bucketAliases: Map<String, String>,
    val s3Client: S3Client,
    val s3AsyncClient: S3AsyncClient,
    val s3Waiter: S3Waiter,
    val s3Presigner: S3Presigner,
    val s3ControlClient: S3ControlClient,
    val s3ControlAsyncClient: S3ControlAsyncClient,
    val s3TransferManager: S3TransferManager,
) {
    /** 存储桶相关操作。 */
    val buckets: BucketOperations = BucketOperations(s3Client, s3Waiter, ::resolveBucketName)

    /** 对象相关操作。 */
    val objects: ObjectOperations = ObjectOperations(s3Client, ::resolveBucketName)

    /** Multipart 相关操作。 */
    val multipart: MultipartOperations = MultipartOperations(s3Client, ::resolveBucketName)

    /** 预签名相关操作。 */
    val presign: PresignOperations = PresignOperations(s3Presigner, ::resolveBucketName)

    /** Transfer Manager 相关操作。 */
    val transfer: TransferOperations = TransferOperations(s3TransferManager, ::resolveBucketName)

    /** 将桶别名解析成真实桶名。 */
    fun resolveBucketName(bucketNameOrAlias: String): String = bucketAliases[bucketNameOrAlias] ?: bucketNameOrAlias
}
