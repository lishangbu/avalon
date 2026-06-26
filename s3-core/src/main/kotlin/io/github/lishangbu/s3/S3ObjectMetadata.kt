package io.github.lishangbu.s3

import java.time.Instant

/**
 * 不下载内容时读取到的 S3 object 元数据。
 */
data class S3ObjectMetadata(
	val key: S3ObjectKey,
	val contentType: String?,
	val metadata: Map<String, String>,
	val eTag: String?,
	val contentLength: Long,
	val lastModified: Instant?,
)
