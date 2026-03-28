package io.github.lishangbu.avalon.s3.facade

import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse
import software.amazon.awssdk.services.s3.model.CompletedPart
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse
import software.amazon.awssdk.services.s3.model.ListPartsRequest
import software.amazon.awssdk.services.s3.model.ListPartsResponse
import software.amazon.awssdk.services.s3.model.UploadPartRequest
import software.amazon.awssdk.services.s3.model.UploadPartResponse

/** Multipart 上传 facade。 */
class MultipartOperations(
    private val s3Client: S3Client,
    private val bucketNameResolver: (String) -> String,
) {
    /** 创建 multipart 上传。 */
    fun create(request: CreateMultipartUploadRequest): CreateMultipartUploadResponse = s3Client.createMultipartUpload(request)

    /** 基于桶和 key 创建 multipart 上传。 */
    fun create(
        bucketName: String,
        key: String,
        contentType: String? = null,
    ): CreateMultipartUploadResponse =
        create(
            CreateMultipartUploadRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .key(key)
                .contentType(contentType)
                .build(),
        )

    /** 上传 part。 */
    fun uploadPart(
        request: UploadPartRequest,
        requestBody: RequestBody,
    ): UploadPartResponse = s3Client.uploadPart(request, requestBody)

    /** 基于桶和 key 上传 part。 */
    fun uploadPart(
        bucketName: String,
        key: String,
        uploadId: String,
        partNumber: Int,
        content: ByteArray,
    ): UploadPartResponse =
        uploadPart(
            UploadPartRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .key(key)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .contentLength(content.size.toLong())
                .build(),
            RequestBody.fromBytes(content),
        )

    /** 列出已上传 part。 */
    fun listParts(request: ListPartsRequest): ListPartsResponse = s3Client.listParts(request)

    /** 列出已上传 part。 */
    fun listParts(
        bucketName: String,
        key: String,
        uploadId: String,
    ): ListPartsResponse =
        listParts(
            ListPartsRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .key(key)
                .uploadId(uploadId)
                .build(),
        )

    /** 完成 multipart 上传。 */
    fun complete(request: CompleteMultipartUploadRequest): CompleteMultipartUploadResponse = s3Client.completeMultipartUpload(request)

    /** 基于桶和 key 完成 multipart 上传。 */
    fun complete(
        bucketName: String,
        key: String,
        uploadId: String,
        completedParts: List<CompletedPart>,
    ): CompleteMultipartUploadResponse =
        complete(
            CompleteMultipartUploadRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .key(key)
                .uploadId(uploadId)
                .multipartUpload { upload -> upload.parts(completedParts) }
                .build(),
        )

    /** 中止 multipart 上传。 */
    fun abort(request: AbortMultipartUploadRequest): AbortMultipartUploadResponse = s3Client.abortMultipartUpload(request)

    /** 基于桶和 key 中止 multipart 上传。 */
    fun abort(
        bucketName: String,
        key: String,
        uploadId: String,
    ): AbortMultipartUploadResponse =
        abort(
            AbortMultipartUploadRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .key(key)
                .uploadId(uploadId)
                .build(),
        )

    private fun resolveBucket(bucketName: String): String = bucketNameResolver(bucketName)
}
