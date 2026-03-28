package io.github.lishangbu.avalon.s3.facade

import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.model.CompletedCopy
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryUpload
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload
import software.amazon.awssdk.transfer.s3.model.CopyRequest
import software.amazon.awssdk.transfer.s3.model.DirectoryDownload
import software.amazon.awssdk.transfer.s3.model.DirectoryUpload
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest
import software.amazon.awssdk.transfer.s3.model.FileDownload
import software.amazon.awssdk.transfer.s3.model.FileUpload
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

/** Transfer Manager facade。 */
class TransferOperations(
    private val transferManager: S3TransferManager,
    private val bucketNameResolver: (String) -> String,
) {
    /** 上传单文件。 */
    fun uploadFile(request: UploadFileRequest): FileUpload = transferManager.uploadFile(request)

    /** 基于桶和 key 上传单文件。 */
    fun uploadFile(
        bucketName: String,
        key: String,
        source: Path,
        contentType: String? = null,
    ): FileUpload =
        uploadFile(
            UploadFileRequest
                .builder()
                .putObjectRequest(
                    PutObjectRequest
                        .builder()
                        .bucket(resolveBucket(bucketName))
                        .key(key)
                        .contentType(contentType)
                        .build(),
                ).source(source)
                .build(),
        )

    /** 下载单文件。 */
    fun downloadFile(request: DownloadFileRequest): FileDownload = transferManager.downloadFile(request)

    /** 基于桶和 key 下载单文件。 */
    fun downloadFile(
        bucketName: String,
        key: String,
        destination: Path,
    ): FileDownload =
        downloadFile(
            DownloadFileRequest
                .builder()
                .getObjectRequest(
                    GetObjectRequest
                        .builder()
                        .bucket(resolveBucket(bucketName))
                        .key(key)
                        .build(),
                ).destination(destination)
                .build(),
        )

    /** 上传目录。 */
    fun uploadDirectory(request: UploadDirectoryRequest): DirectoryUpload = transferManager.uploadDirectory(request)

    /** 基于桶上传目录。 */
    fun uploadDirectory(
        bucketName: String,
        source: Path,
        prefix: String = "",
    ): DirectoryUpload =
        uploadDirectory(
            UploadDirectoryRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .source(source)
                .s3Prefix(prefix)
                .build(),
        )

    /** 下载目录。 */
    fun downloadDirectory(request: DownloadDirectoryRequest): DirectoryDownload = transferManager.downloadDirectory(request)

    /** 基于桶下载目录。 */
    fun downloadDirectory(
        bucketName: String,
        prefix: String = "",
        destination: Path,
    ): DirectoryDownload =
        downloadDirectory(
            DownloadDirectoryRequest
                .builder()
                .bucket(resolveBucket(bucketName))
                .listObjectsV2RequestTransformer { request ->
                    request.prefix(prefix)
                }.destination(destination)
                .build(),
        )

    /** 复制对象。 */
    fun copy(request: CopyRequest): CompletableFuture<CompletedCopy> = transferManager.copy(request).completionFuture()

    /** 基于桶和 key 复制对象。 */
    fun copy(
        sourceBucketName: String,
        sourceKey: String,
        destinationBucketName: String,
        destinationKey: String,
    ): CompletableFuture<CompletedCopy> =
        copy(
            CopyRequest
                .builder()
                .copyObjectRequest(
                    CopyObjectRequest
                        .builder()
                        .sourceBucket(resolveBucket(sourceBucketName))
                        .sourceKey(sourceKey)
                        .destinationBucket(resolveBucket(destinationBucketName))
                        .destinationKey(destinationKey)
                        .build(),
                ).build(),
        )

    /** 等待单文件上传完成。 */
    fun await(upload: FileUpload): CompletedFileUpload = upload.completionFuture().join()

    /** 等待单文件下载完成。 */
    fun await(download: FileDownload): CompletedFileDownload = download.completionFuture().join()

    /** 等待目录上传完成。 */
    fun await(upload: DirectoryUpload): CompletedDirectoryUpload = upload.completionFuture().join()

    /** 等待目录下载完成。 */
    fun await(download: DirectoryDownload): CompletedDirectoryDownload = download.completionFuture().join()

    private fun resolveBucket(bucketName: String): String = bucketNameResolver(bucketName)
}
