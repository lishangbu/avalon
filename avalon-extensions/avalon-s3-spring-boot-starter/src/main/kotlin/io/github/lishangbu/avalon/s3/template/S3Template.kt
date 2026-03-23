package io.github.lishangbu.avalon.s3.template

import io.github.lishangbu.avalon.s3.properties.S3Properties
import org.springframework.beans.factory.InitializingBean
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.io.InputStream
import java.net.URI
import java.time.Duration
import java.util.*

/**
 * S3 操作模板 提供基于 AWS SDK v2 的常用 S3 操作封装，包括：
 * - 桶（bucket）管理：创建/查询/删除
 * - 对象（object）管理：上传/下载/删除/列举
 * - 生成签名 URL（用于上传/下载） 设计要点：
 * - 通过 `S3Properties` 注入配置
 * - 延迟初始化 `S3Client` 与 `S3Presigner`，在 `afterPropertiesSet` 中创建
 * - 兼容 path-style 与 virtual-hosted-style
 *
 * @param bucketName bucket 名称 判断 bucket 是否存在
 * @param prefix 前缀
 * @param objectName 对象名称
 * @param minutes 过期分钟（必须小于7天）
 * @param expires 过期时长（必须小于7天） 获取对象上传链接（过期分钟）
 * @param minutes 过期分钟
 * @param httpMethod HTTP 方法
 * @param expires 过期时长
 * @param httpMethod HTTP 方法（GET/PUT） 获取对象公共访问 URL 若对象设置了公共读权限，该 URL 可直接访问对象数据
 * @param stream 文件流
 * @param bucketName bucket 名称
 * @param objectName 对象名称
 * @param contextType Content-Type
 * @param stream 文件流 上传对象
 * @param size 大小
 * @return 是否存在 获取全部 bucket 列表
 * @return bucket 列表 根据名称查找 bucket（兼容性封装）
 * @return 可选的 bucket 删除 bucket 根据前缀列举对象
 * @return 对象列表 获取对象下载链接（过期分钟）
 * @return 下载链接 获取对象下载链接（过期时长）
 * @return 上传链接 获取对象上传链接（过期时长） v1 兼容：获取对象链接（指定 HTTP 方法）
 * @return 链接 获取对象链接（支持自定义 HTTP 方法）
 * @return URL 获取对象输入流
 * @return InputStream 上传对象（自动推断大小）
 * @return PutObjectResponse 获取对象元信息
 * @return HeadObjectResponse 删除对象 创建 S3 客户端 创建 S3 Presigner // 创建 S3 客户端 // 创建 S3 Presigner
 * @throws IOException IO 异常 上传对象（指定 Content-Type）
 * @author lishangbu
 * @since 2026/1/18 创建 bucket
 */

/**
 * S3 操作模板。
 *
 * 提供基于 AWS SDK v2 的常用 S3 操作封装。
 */
class S3Template(
    private val s3Properties: S3Properties,
) : InitializingBean {
    private lateinit var s3Client: S3Client
    private lateinit var s3Presigner: S3Presigner

    fun createBucket(bucketName: String) {
        if (!headBucket(bucketName)) {
            val request = CreateBucketRequest.builder().bucket(bucketName).build()
            s3Client.createBucket(request)
        }
    }

    fun headBucket(bucketName: String): Boolean =
        try {
            val request = HeadBucketRequest.builder().bucket(bucketName).build()
            s3Client.headBucket(request)
            true
        } catch (_: NoSuchBucketException) {
            false
        }

    fun getAllBuckets(): List<Bucket> {
        val response: ListBucketsResponse = s3Client.listBuckets()
        return response.buckets()
    }

    fun getBucket(bucketName: String): Optional<Bucket> = Optional.ofNullable(getAllBuckets().firstOrNull { it.name() == bucketName })

    fun removeBucket(bucketName: String) {
        val request = DeleteBucketRequest.builder().bucket(bucketName).build()
        s3Client.deleteBucket(request)
    }

    fun getAllObjectsByPrefix(
        bucketName: String,
        prefix: String,
    ): List<S3Object> {
        val request =
            ListObjectsV2Request
                .builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build()
        val response: ListObjectsV2Response = s3Client.listObjectsV2(request)
        return response.contents()
    }

    fun getObjectURL(
        bucketName: String,
        objectName: String,
        minutes: Int,
    ): String = getObjectURL(bucketName, objectName, Duration.ofMinutes(minutes.toLong()))

    fun getObjectURL(
        bucketName: String,
        objectName: String,
        expires: Duration,
    ): String {
        val getObjectRequest =
            GetObjectRequest
                .builder()
                .bucket(bucketName)
                .key(objectName)
                .build()

        val presignRequest =
            GetObjectPresignRequest
                .builder()
                .signatureDuration(expires)
                .getObjectRequest(getObjectRequest)
                .build()

        val presignedRequest: PresignedGetObjectRequest =
            s3Presigner.presignGetObject(presignRequest)
        return presignedRequest.url().toString()
    }

    fun getPutObjectURL(
        bucketName: String,
        objectName: String,
        minutes: Int,
    ): String = getPutObjectURL(bucketName, objectName, Duration.ofMinutes(minutes.toLong()))

    fun getPutObjectURL(
        bucketName: String,
        objectName: String,
        expires: Duration,
    ): String {
        val putObjectRequest =
            PutObjectRequest
                .builder()
                .bucket(bucketName)
                .key(objectName)
                .build()

        val presignRequest =
            PutObjectPresignRequest
                .builder()
                .signatureDuration(expires)
                .putObjectRequest(putObjectRequest)
                .build()

        val presignedRequest: PresignedPutObjectRequest =
            s3Presigner.presignPutObject(presignRequest)
        return presignedRequest.url().toString()
    }

    fun getObjectURL(
        bucketName: String,
        objectName: String,
        minutes: Int,
        httpMethod: String,
    ) = getObjectURL(bucketName, objectName, Duration.ofMinutes(minutes.toLong()), httpMethod)

    fun getObjectURL(
        bucketName: String,
        objectName: String,
        expires: Duration,
        httpMethod: String,
    ): String =
        if (httpMethod.equals("PUT", ignoreCase = true)) {
            getPutObjectURL(bucketName, objectName, expires)
        } else {
            getObjectURL(bucketName, objectName, expires)
        }

    fun getObjectURL(
        bucketName: String,
        objectName: String,
    ): String = "${requireProperty(s3Properties.endpoint, "s3.endpoint")}/$bucketName/$objectName"

    fun getObject(
        bucketName: String,
        objectName: String,
    ): InputStream {
        val request =
            GetObjectRequest
                .builder()
                .bucket(bucketName)
                .key(objectName)
                .build()
        return s3Client.getObject(request, ResponseTransformer.toInputStream())
    }

    fun putObject(
        bucketName: String,
        objectName: String,
        stream: InputStream,
    ) {
        putObject(bucketName, objectName, stream.readBytes(), "application/octet-stream")
    }

    fun putObject(
        bucketName: String,
        objectName: String,
        contextType: String,
        stream: InputStream,
    ) {
        putObject(bucketName, objectName, stream.readBytes(), contextType)
    }

    fun putObject(
        bucketName: String,
        objectName: String,
        stream: InputStream,
        size: Long,
        contextType: String,
    ): PutObjectResponse {
        val request =
            PutObjectRequest
                .builder()
                .bucket(bucketName)
                .key(objectName)
                .contentType(contextType)
                .contentLength(size)
                .build()

        return s3Client.putObject(request, RequestBody.fromInputStream(stream, size))
    }

    private fun putObject(
        bucketName: String,
        objectName: String,
        content: ByteArray,
        contextType: String,
    ): PutObjectResponse {
        val request =
            PutObjectRequest
                .builder()
                .bucket(bucketName)
                .key(objectName)
                .contentType(contextType)
                .contentLength(content.size.toLong())
                .build()

        return s3Client.putObject(request, RequestBody.fromBytes(content))
    }

    fun getObjectInfo(
        bucketName: String,
        objectName: String,
    ): HeadObjectResponse {
        val request =
            HeadObjectRequest
                .builder()
                .bucket(bucketName)
                .key(objectName)
                .build()
        return s3Client.headObject(request)
    }

    fun removeObject(
        bucketName: String,
        objectName: String,
    ) {
        val request =
            DeleteObjectRequest
                .builder()
                .bucket(bucketName)
                .key(objectName)
                .build()
        s3Client.deleteObject(request)
    }

    override fun afterPropertiesSet() {
        s3Client =
            S3Client
                .builder()
                .endpointOverride(endpointUri())
                .region(resolveRegion())
                .credentialsProvider(StaticCredentialsProvider.create(basicCredentials()))
                .serviceConfiguration(
                    S3Configuration
                        .builder()
                        .pathStyleAccessEnabled(s3Properties.pathStyleAccess)
                        .chunkedEncodingEnabled(s3Properties.chunkedEncodingEnabled)
                        .build(),
                ).build()

        s3Presigner =
            S3Presigner
                .builder()
                .endpointOverride(endpointUri())
                .region(resolveRegion())
                .credentialsProvider(StaticCredentialsProvider.create(basicCredentials()))
                .serviceConfiguration(
                    S3Configuration
                        .builder()
                        .pathStyleAccessEnabled(s3Properties.pathStyleAccess)
                        .build(),
                ).build()
    }

    private fun endpointUri(): URI = URI.create(requireProperty(s3Properties.endpoint, "s3.endpoint"))

    private fun resolveRegion(): Region = Region.of(s3Properties.region?.ifBlank { null } ?: "us-east-1")

    private fun basicCredentials(): AwsBasicCredentials =
        AwsBasicCredentials.create(
            requireProperty(s3Properties.accessKey, "s3.access-key"),
            requireProperty(s3Properties.secretKey, "s3.secret-key"),
        )

    private fun requireProperty(
        value: String?,
        propertyName: String,
    ): String =
        value?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "Property '$propertyName' must be configured for S3Template.",
            )
}
