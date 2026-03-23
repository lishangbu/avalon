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

/**
 * S3 操作模板
 *
 * 封装常用的存储桶、对象和预签名 URL 操作
 */
class S3Template(
    /** S3 属性 */
    private val s3Properties: S3Properties,
) : InitializingBean {
    /** S3 客户端 */
    private lateinit var s3Client: S3Client

    /** S3 预签名器 */
    private lateinit var s3Presigner: S3Presigner

    /** 创建存储桶 */
    fun createBucket(bucketName: String) {
        if (!headBucket(bucketName)) {
            val request = CreateBucketRequest.builder().bucket(bucketName).build()
            s3Client.createBucket(request)
        }
    }

    /** 检查存储桶是否存在 */
    fun headBucket(bucketName: String): Boolean =
        try {
            val request = HeadBucketRequest.builder().bucket(bucketName).build()
            s3Client.headBucket(request)
            true
        } catch (_: NoSuchBucketException) {
            false
        }

    /** 获取所有存储桶 */
    fun getAllBuckets(): List<Bucket> {
        val response: ListBucketsResponse = s3Client.listBuckets()
        return response.buckets()
    }

    /** 按名称获取存储桶 */
    fun getBucket(bucketName: String): Bucket? = getAllBuckets().firstOrNull { it.name() == bucketName }

    /** 删除存储桶 */
    fun removeBucket(bucketName: String) {
        val request = DeleteBucketRequest.builder().bucket(bucketName).build()
        s3Client.deleteBucket(request)
    }

    /** 按前缀列出对象 */
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

    /** 生成对象下载预签名 URL */
    fun getObjectURL(
        bucketName: String,
        objectName: String,
        minutes: Int,
    ): String = getObjectURL(bucketName, objectName, Duration.ofMinutes(minutes.toLong()))

    /** 生成对象下载预签名 URL */
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

    /** 生成对象上传预签名 URL */
    fun getPutObjectURL(
        bucketName: String,
        objectName: String,
        minutes: Int,
    ): String = getPutObjectURL(bucketName, objectName, Duration.ofMinutes(minutes.toLong()))

    /** 生成对象上传预签名 URL */
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

    /** 按 HTTP 方法生成对象预签名 URL */
    fun getObjectURL(
        bucketName: String,
        objectName: String,
        minutes: Int,
        httpMethod: String,
    ) = getObjectURL(bucketName, objectName, Duration.ofMinutes(minutes.toLong()), httpMethod)

    /** 按 HTTP 方法生成对象预签名 URL */
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

    /** 拼接对象访问地址 */
    fun getObjectURL(
        bucketName: String,
        objectName: String,
    ): String = "${requireProperty(s3Properties.endpoint, "s3.endpoint")}/$bucketName/$objectName"

    /** 读取对象内容 */
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

    /** 上传对象 */
    fun putObject(
        bucketName: String,
        objectName: String,
        stream: InputStream,
    ) {
        putObject(bucketName, objectName, stream.readBytes(), "application/octet-stream")
    }

    /** 上传指定内容类型的对象 */
    fun putObject(
        bucketName: String,
        objectName: String,
        contextType: String,
        stream: InputStream,
    ) {
        putObject(bucketName, objectName, stream.readBytes(), contextType)
    }

    /** 上传指定长度的对象 */
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

    /** 上传字节数组内容 */
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

    /** 获取对象元数据 */
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

    /** 删除对象 */
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

    /** 读取配置并创建 S3 客户端与预签名器 */
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

    /** 解析端点 URI */
    private fun endpointUri(): URI = URI.create(requireProperty(s3Properties.endpoint, "s3.endpoint"))

    /** 解析区域 */
    private fun resolveRegion(): Region = Region.of(s3Properties.region?.ifBlank { null } ?: "us-east-1")

    /** 创建基础凭证 */
    private fun basicCredentials(): AwsBasicCredentials =
        AwsBasicCredentials.create(
            requireProperty(s3Properties.accessKey, "s3.access-key"),
            requireProperty(s3Properties.secretKey, "s3.secret-key"),
        )

    /** 校验并返回配置值 */
    private fun requireProperty(
        value: String?,
        propertyName: String,
    ): String =
        value?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "Property '$propertyName' must be configured for S3Template.",
            )
}
