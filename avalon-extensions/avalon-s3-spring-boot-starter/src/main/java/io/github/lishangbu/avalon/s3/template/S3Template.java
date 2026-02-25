package io.github.lishangbu.avalon.s3.template;

import io.github.lishangbu.avalon.s3.properties.S3Properties;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/// S3 操作模板
///
/// 提供基于 AWS SDK v2 的常用 S3 操作封装，包括：
/// - 桶（bucket）管理：创建/查询/删除
/// - 对象（object）管理：上传/下载/删除/列举
/// - 生成签名 URL（用于上传/下载）
///
/// 设计要点：
/// - 通过 `S3Properties` 注入配置
/// - 延迟初始化 `S3Client` 与 `S3Presigner`，在 `afterPropertiesSet` 中创建
/// - 兼容 path-style 与 virtual-hosted-style
///
/// @author lishangbu
/// @since 2026/1/18
@RequiredArgsConstructor
public class S3Template implements InitializingBean {

    private final S3Properties s3Properties;

    private S3Client s3Client;

    private S3Presigner s3Presigner;

    /// 创建 bucket
    ///
    /// @param bucketName bucket 名称
    public void createBucket(String bucketName) {
        if (!headBucket(bucketName)) {
            CreateBucketRequest createBucketRequest =
                    CreateBucketRequest.builder().bucket(bucketName).build();
            s3Client.createBucket(createBucketRequest);
        }
    }

    /// 判断 bucket 是否存在
    ///
    /// @param bucketName bucket 名称
    /// @return 是否存在
    public boolean headBucket(String bucketName) {
        try {
            HeadBucketRequest headBucketRequest =
                    HeadBucketRequest.builder().bucket(bucketName).build();
            s3Client.headBucket(headBucketRequest);
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }

    /// 获取全部 bucket 列表
    ///
    /// @return bucket 列表
    public List<Bucket> getAllBuckets() {
        ListBucketsResponse listBucketsResponse = s3Client.listBuckets();
        return listBucketsResponse.buckets();
    }

    /// 根据名称查找 bucket（兼容性封装）
    ///
    /// @param bucketName bucket 名称
    /// @return 可选的 bucket
    public Optional<Bucket> getBucket(String bucketName) {
        return getAllBuckets().stream().filter(b -> b.name().equals(bucketName)).findFirst();
    }

    /// 删除 bucket
    ///
    /// @param bucketName bucket 名称
    public void removeBucket(String bucketName) {
        DeleteBucketRequest deleteBucketRequest =
                DeleteBucketRequest.builder().bucket(bucketName).build();
        s3Client.deleteBucket(deleteBucketRequest);
    }

    /// 根据前缀列举对象
    ///
    /// @param bucketName bucket 名称
    /// @param prefix     前缀
    /// @return 对象列表
    public List<S3Object> getAllObjectsByPrefix(String bucketName, String prefix) {
        ListObjectsV2Request listObjectsRequest =
                ListObjectsV2Request.builder().bucket(bucketName).prefix(prefix).build();

        ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(listObjectsRequest);
        return listObjectsResponse.contents();
    }

    /// 获取对象下载链接（过期分钟）
    ///
    /// @param bucketName bucket 名称
    /// @param objectName 对象名称
    /// @param minutes    过期分钟（必须小于7天）
    /// @return 下载链接
    public String getObjectURL(String bucketName, String objectName, int minutes) {
        return getObjectURL(bucketName, objectName, Duration.ofMinutes(minutes));
    }

    /// 获取对象下载链接（过期时长）
    ///
    /// @param bucketName bucket 名称
    /// @param objectName 对象名称
    /// @param expires    过期时长（必须小于7天）
    /// @return 下载链接
    public String getObjectURL(String bucketName, String objectName, Duration expires) {
        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder().bucket(bucketName).key(objectName).build();

        GetObjectPresignRequest getObjectPresignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(expires)
                        .getObjectRequest(getObjectRequest)
                        .build();

        PresignedGetObjectRequest presignedGetObjectRequest =
                s3Presigner.presignGetObject(getObjectPresignRequest);
        return presignedGetObjectRequest.url().toString();
    }

    /// 获取对象上传链接（过期分钟）
    ///
    /// @param bucketName bucket 名称
    /// @param objectName 对象名称
    /// @param minutes    过期分钟（必须小于7天）
    /// @return 上传链接
    public String getPutObjectURL(String bucketName, String objectName, int minutes) {
        return getPutObjectURL(bucketName, objectName, Duration.ofMinutes(minutes));
    }

    /// 获取对象上传链接（过期时长）
    ///
    /// @param bucketName bucket 名称
    /// @param objectName 对象名称
    /// @param expires    过期时长（必须小于7天）
    /// @return 上传链接
    public String getPutObjectURL(String bucketName, String objectName, Duration expires) {
        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder().bucket(bucketName).key(objectName).build();

        PutObjectPresignRequest putObjectPresignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(expires)
                        .putObjectRequest(putObjectRequest)
                        .build();

        PresignedPutObjectRequest presignedPutObjectRequest =
                s3Presigner.presignPutObject(putObjectPresignRequest);
        return presignedPutObjectRequest.url().toString();
    }

    /// v1 兼容：获取对象链接（指定 HTTP 方法）
    ///
    /// @param bucketName bucket 名称
    /// @param objectName 对象名称
    /// @param minutes    过期分钟
    /// @param httpMethod HTTP 方法
    /// @return 链接
    public String getObjectURL(
            String bucketName, String objectName, int minutes, String httpMethod) {
        return getObjectURL(bucketName, objectName, Duration.ofMinutes(minutes), httpMethod);
    }

    /// 获取对象链接（支持自定义 HTTP 方法）
    ///
    /// @param bucketName bucket 名称
    /// @param objectName 对象名称
    /// @param expires    过期时长
    /// @param httpMethod HTTP 方法（GET/PUT）
    /// @return 链接
    public String getObjectURL(
            String bucketName, String objectName, Duration expires, String httpMethod) {
        if ("PUT".equalsIgnoreCase(httpMethod)) {
            return getPutObjectURL(bucketName, objectName, expires);
        } else {
            return getObjectURL(bucketName, objectName, expires);
        }
    }

    /// 获取对象公共访问 URL
    ///
    /// 若对象设置了公共读权限，该 URL 可直接访问对象数据
    ///
    /// @param bucketName bucket 名称
    /// @param objectName 对象名称
    /// @return URL
    public String getObjectURL(String bucketName, String objectName) {
        return String.format("%s/%s/%s", s3Properties.getEndpoint(), bucketName, objectName);
    }

    /// 获取对象输入流
    ///
    /// @param bucketName bucket 名称
    /// @param objectName 对象名称
    /// @return InputStream
    public InputStream getObject(String bucketName, String objectName) {
        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder().bucket(bucketName).key(objectName).build();

        return s3Client.getObject(getObjectRequest, ResponseTransformer.toInputStream());
    }

    /// 上传对象（自动推断大小）
    ///
    /// @param bucketName bucket 名称
    /// @param objectName 对象名称
    /// @param stream     文件流
    /// @throws IOException IO 异常
    public void putObject(String bucketName, String objectName, InputStream stream)
            throws IOException {
        putObject(bucketName, objectName, stream, stream.available(), "application/octet-stream");
    }

    /// 上传对象（指定 Content-Type）
    ///
    /// @param bucketName  bucket 名称
    /// @param objectName  对象名称
    /// @param contextType Content-Type
    /// @param stream      文件流
    /// @throws IOException IO 异常
    public void putObject(
            String bucketName, String objectName, String contextType, InputStream stream)
            throws IOException {
        putObject(bucketName, objectName, stream, stream.available(), contextType);
    }

    /// 上传对象
    ///
    /// @param bucketName  bucket 名称
    /// @param objectName  对象名称
    /// @param stream      文件流
    /// @param size        大小
    /// @param contextType Content-Type
    /// @return PutObjectResponse
    public PutObjectResponse putObject(
            String bucketName,
            String objectName,
            InputStream stream,
            long size,
            String contextType) {
        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectName)
                        .contentType(contextType)
                        .contentLength(size)
                        .build();

        return s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(stream, size));
    }

    /// 获取对象元信息
    ///
    /// @param bucketName bucket 名称
    /// @param objectName 对象名称
    /// @return HeadObjectResponse
    public HeadObjectResponse getObjectInfo(String bucketName, String objectName) {
        HeadObjectRequest headObjectRequest =
                HeadObjectRequest.builder().bucket(bucketName).key(objectName).build();

        return s3Client.headObject(headObjectRequest);
    }

    /// 删除对象
    ///
    /// @param bucketName bucket 名称
    /// @param objectName 对象名称
    public void removeObject(String bucketName, String objectName) {
        DeleteObjectRequest deleteObjectRequest =
                DeleteObjectRequest.builder().bucket(bucketName).key(objectName).build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 创建 S3 客户端
        this.s3Client =
                S3Client.builder()
                        .endpointOverride(URI.create(s3Properties.getEndpoint()))
                        .region(
                                Region.of(
                                        s3Properties.getRegion() != null
                                                ? s3Properties.getRegion()
                                                : "us-east-1"))
                        .credentialsProvider(
                                StaticCredentialsProvider.create(
                                        AwsBasicCredentials.create(
                                                s3Properties.getAccessKey(),
                                                s3Properties.getSecretKey())))
                        .serviceConfiguration(
                                S3Configuration.builder()
                                        .pathStyleAccessEnabled(s3Properties.getPathStyleAccess())
                                        .chunkedEncodingEnabled(
                                                s3Properties.getChunkedEncodingEnabled())
                                        .build())
                        .build();

        // 创建 S3 Presigner
        this.s3Presigner =
                S3Presigner.builder()
                        .endpointOverride(URI.create(s3Properties.getEndpoint()))
                        .region(
                                Region.of(
                                        s3Properties.getRegion() != null
                                                ? s3Properties.getRegion()
                                                : "us-east-1"))
                        .credentialsProvider(
                                StaticCredentialsProvider.create(
                                        AwsBasicCredentials.create(
                                                s3Properties.getAccessKey(),
                                                s3Properties.getSecretKey())))
                        .serviceConfiguration(
                                S3Configuration.builder()
                                        .pathStyleAccessEnabled(s3Properties.getPathStyleAccess())
                                        .build())
                        .build();
    }
}
