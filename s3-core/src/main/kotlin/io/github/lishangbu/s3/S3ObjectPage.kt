package io.github.lishangbu.s3

/**
 * 一页 S3 object 列举结果。
 */
data class S3ObjectPage(
	val objects: List<S3ObjectSummary>,
	val nextContinuationToken: String?,
	val truncated: Boolean,
)
