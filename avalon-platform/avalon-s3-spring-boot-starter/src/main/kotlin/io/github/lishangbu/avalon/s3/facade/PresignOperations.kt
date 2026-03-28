package io.github.lishangbu.avalon.s3.facade

import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.UploadPartRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.DeleteObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.HeadObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedDeleteObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedHeadObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest
import java.time.Duration

/** 预签名 facade。 */
class PresignOperations(
    private val presigner: S3Presigner,
    private val bucketNameResolver: (String) -> String,
) {
    /** 预签名 GET。 */
    fun get(
        request: GetObjectRequest,
        expiresIn: Duration,
    ): PresignedGetObjectRequest =
        presigner.presignGetObject(
            GetObjectPresignRequest
                .builder()
                .signatureDuration(expiresIn)
                .getObjectRequest(request)
                .build(),
        )

    /** 基于桶和 key 预签名 GET。 */
    fun get(
        bucketName: String,
        key: String,
        expiresIn: Duration,
    ): PresignedGetObjectRequest =
        get(
            GetObjectRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .key(key)
                .build(),
            expiresIn,
        )

    /** 预签名 PUT。 */
    fun put(
        request: PutObjectRequest,
        expiresIn: Duration,
    ): PresignedPutObjectRequest =
        presigner.presignPutObject(
            PutObjectPresignRequest
                .builder()
                .signatureDuration(expiresIn)
                .putObjectRequest(request)
                .build(),
        )

    /** 基于桶和 key 预签名 PUT。 */
    fun put(
        bucketName: String,
        key: String,
        expiresIn: Duration,
        contentType: String? = null,
    ): PresignedPutObjectRequest =
        put(
            PutObjectRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .key(key)
                .contentType(contentType)
                .build(),
            expiresIn,
        )

    /** 预签名 DELETE。 */
    fun delete(
        request: DeleteObjectRequest,
        expiresIn: Duration,
    ): PresignedDeleteObjectRequest =
        presigner.presignDeleteObject(
            DeleteObjectPresignRequest
                .builder()
                .signatureDuration(expiresIn)
                .deleteObjectRequest(request)
                .build(),
        )

    /** 基于桶和 key 预签名 DELETE。 */
    fun delete(
        bucketName: String,
        key: String,
        expiresIn: Duration,
    ): PresignedDeleteObjectRequest =
        delete(
            DeleteObjectRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .key(key)
                .build(),
            expiresIn,
        )

    /** 预签名 HEAD。 */
    fun head(
        request: HeadObjectRequest,
        expiresIn: Duration,
    ): PresignedHeadObjectRequest =
        presigner.presignHeadObject(
            HeadObjectPresignRequest
                .builder()
                .signatureDuration(expiresIn)
                .headObjectRequest(request)
                .build(),
        )

    /** 基于桶和 key 预签名 HEAD。 */
    fun head(
        bucketName: String,
        key: String,
        expiresIn: Duration,
    ): PresignedHeadObjectRequest =
        head(
            HeadObjectRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .key(key)
                .build(),
            expiresIn,
        )

    /** 预签名上传 part。 */
    fun uploadPart(
        request: UploadPartRequest,
        expiresIn: Duration,
    ): PresignedUploadPartRequest =
        presigner.presignUploadPart(
            UploadPartPresignRequest
                .builder()
                .signatureDuration(expiresIn)
                .uploadPartRequest(request)
                .build(),
        )

    /** 基于桶和 key 预签名上传 part。 */
    fun uploadPart(
        bucketName: String,
        key: String,
        uploadId: String,
        partNumber: Int,
        expiresIn: Duration,
    ): PresignedUploadPartRequest =
        uploadPart(
            UploadPartRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .key(key)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .build(),
            expiresIn,
        )

    private fun resolveBucket(bucketName: String): String = bucketNameResolver(bucketName)
}
