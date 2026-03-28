package io.github.lishangbu.avalon.s3.facade

import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.CopyObjectResponse
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import java.io.InputStream

/** 对象操作 facade。 */
class ObjectOperations(
    private val s3Client: S3Client,
    private val bucketNameResolver: (String) -> String,
) {
    /** 透传原生 putObject。 */
    fun put(
        request: PutObjectRequest,
        requestBody: RequestBody,
    ): PutObjectResponse = s3Client.putObject(request, requestBody)

    /** 上传字节数组对象。 */
    fun put(
        bucketName: String,
        key: String,
        content: ByteArray,
        contentType: String = "application/octet-stream",
    ): PutObjectResponse =
        put(
            PutObjectRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .key(key)
                .contentType(contentType)
                .contentLength(content.size.toLong())
                .build(),
            RequestBody.fromBytes(content),
        )

    /** 上传输入流对象。 */
    fun put(
        bucketName: String,
        key: String,
        stream: InputStream,
        contentLength: Long,
        contentType: String = "application/octet-stream",
    ): PutObjectResponse =
        put(
            PutObjectRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build(),
            RequestBody.fromInputStream(stream, contentLength),
        )

    /** 透传原生 getObject 输入流。 */
    fun get(request: GetObjectRequest): ResponseInputStream<GetObjectResponse> = s3Client.getObject(request, ResponseTransformer.toInputStream())

    /** 下载为输入流。 */
    fun get(
        bucketName: String,
        key: String,
    ): ResponseInputStream<GetObjectResponse> =
        get(
            GetObjectRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .key(key)
                .build(),
        )

    /** 下载为字节数组。 */
    fun getBytes(
        bucketName: String,
        key: String,
    ): ResponseBytes<GetObjectResponse> =
        s3Client.getObjectAsBytes(
            GetObjectRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .key(key)
                .build(),
        )

    /** 查询对象元数据。 */
    fun head(request: HeadObjectRequest): HeadObjectResponse = s3Client.headObject(request)

    /** 查询对象元数据。 */
    fun head(
        bucketName: String,
        key: String,
    ): HeadObjectResponse =
        head(
            HeadObjectRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .key(key)
                .build(),
        )

    /** 删除对象。 */
    fun delete(request: DeleteObjectRequest): DeleteObjectResponse = s3Client.deleteObject(request)

    /** 删除对象。 */
    fun delete(
        bucketName: String,
        key: String,
    ): DeleteObjectResponse =
        delete(
            DeleteObjectRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .key(key)
                .build(),
        )

    /** 列举对象。 */
    fun list(request: ListObjectsV2Request): ListObjectsV2Response = s3Client.listObjectsV2(request)

    /** 按前缀列举对象。 */
    fun list(
        bucketName: String,
        prefix: String? = null,
    ): ListObjectsV2Response =
        list(
            ListObjectsV2Request
                .builder()
                .bucket(resolveBucket(bucketName))
                .prefix(prefix)
                .build(),
        )

    /** 复制对象。 */
    fun copy(request: CopyObjectRequest): CopyObjectResponse = s3Client.copyObject(request)

    /** 按桶和 key 复制对象。 */
    fun copy(
        sourceBucketName: String,
        sourceKey: String,
        destinationBucketName: String,
        destinationKey: String,
    ): CopyObjectResponse =
        copy(
            CopyObjectRequest
                .builder()
                .sourceBucket(resolveBucket(sourceBucketName))
                .sourceKey(sourceKey)
                .destinationBucket(resolveBucket(destinationBucketName))
                .destinationKey(destinationKey)
                .build(),
        )

    private fun resolveBucket(bucketName: String): String = bucketNameResolver(bucketName)
}
