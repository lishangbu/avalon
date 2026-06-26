package io.github.lishangbu.s3

import java.time.Duration

/**
 * 创建预签名下载 URL 的命令参数。
 */
data class S3PresignGetObjectCommand(
	val key: S3ObjectKey,
	val ttl: Duration,
) {
	init {
		require(!ttl.isZero && !ttl.isNegative) {
			"S3 预签名下载 URL 过期时间必须大于 0"
		}
	}
}
