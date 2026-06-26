package io.github.lishangbu.s3

import java.time.Duration

/**
 * 创建预签名上传 URL 的命令参数。
 */
data class S3PresignPutObjectCommand(
	val key: S3ObjectKey,
	val ttl: Duration,
	val contentType: String? = null,
	val metadata: Map<String, String> = emptyMap(),
) {
	init {
		require(!ttl.isZero && !ttl.isNegative) {
			"S3 预签名上传 URL 过期时间必须大于 0"
		}
	}
}
