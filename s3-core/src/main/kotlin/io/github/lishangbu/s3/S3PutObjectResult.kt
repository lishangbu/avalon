package io.github.lishangbu.s3

/**
 * 上传对象后的 S3 响应摘要。
 */
data class S3PutObjectResult(
	val key: S3ObjectKey,
	val eTag: String?,
	val versionId: String?,
)
