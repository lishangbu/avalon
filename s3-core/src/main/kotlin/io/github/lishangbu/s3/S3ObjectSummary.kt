package io.github.lishangbu.s3

import java.time.Instant

/**
 * 列举对象时返回的轻量摘要。
 */
data class S3ObjectSummary(
	val key: S3ObjectKey,
	val eTag: String?,
	val size: Long,
	val lastModified: Instant?,
)
