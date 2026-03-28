package io.github.lishangbu.avalon.s3.facade

import software.amazon.awssdk.core.waiters.WaiterResponse
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.CreateBucketResponse
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest
import software.amazon.awssdk.services.s3.model.DeleteBucketResponse
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.HeadBucketResponse
import software.amazon.awssdk.services.s3.model.ListBucketsRequest
import software.amazon.awssdk.services.s3.model.ListBucketsResponse
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.waiters.S3Waiter

/** 存储桶操作 facade。 */
class BucketOperations(
    private val s3Client: S3Client,
    private val s3Waiter: S3Waiter,
    private val bucketNameResolver: (String) -> String,
) {
    /** 透传原生创建桶请求。 */
    fun create(request: CreateBucketRequest): CreateBucketResponse = s3Client.createBucket(request)

    /** 基于桶名创建桶。 */
    fun create(bucketName: String): CreateBucketResponse =
        create(
            CreateBucketRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .build(),
        )

    /** 透传原生删除桶请求。 */
    fun delete(request: DeleteBucketRequest): DeleteBucketResponse = s3Client.deleteBucket(request)

    /** 基于桶名删除桶。 */
    fun delete(bucketName: String): DeleteBucketResponse =
        delete(
            DeleteBucketRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .build(),
        )

    /** 透传原生查询桶请求。 */
    fun head(request: HeadBucketRequest): HeadBucketResponse = s3Client.headBucket(request)

    /** 基于桶名查询桶。 */
    fun head(bucketName: String): HeadBucketResponse =
        head(
            HeadBucketRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .build(),
        )

    /** 判断桶是否存在。 */
    fun exists(bucketName: String): Boolean =
        try {
            head(bucketName)
            true
        } catch (exception: S3Exception) {
            if (exception.statusCode() != 404 && exception.statusCode() != 301) {
                throw exception
            }
            false
        }

    /** 列出所有桶。 */
    fun list(request: ListBucketsRequest = ListBucketsRequest.builder().build()): ListBucketsResponse = s3Client.listBuckets(request)

    /** 等待桶创建完成。 */
    fun waitUntilExists(bucketName: String): WaiterResponse<HeadBucketResponse> =
        s3Waiter.waitUntilBucketExists(
            HeadBucketRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .build(),
        )

    /** 等待桶删除完成。 */
    fun waitUntilMissing(bucketName: String): WaiterResponse<HeadBucketResponse> =
        s3Waiter.waitUntilBucketNotExists(
            HeadBucketRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .build(),
        )

    private fun resolveBucket(bucketName: String): String = bucketNameResolver(bucketName)
}
