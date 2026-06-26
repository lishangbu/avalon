package io.github.lishangbu.s3

import java.time.Duration

/**
 * [S3Operations] 运行所需的存储桶和 key 前缀。
 */
data class S3ClientSettings(
	val bucket: String,
	val keyPrefix: String = "",
	val defaultPresignTtl: Duration = Duration.ofMinutes(15),
) {
	init {
		require(bucket.isNotBlank()) {
			"S3 bucket 不能为空"
		}
		require(!defaultPresignTtl.isZero && !defaultPresignTtl.isNegative) {
			"S3 预签名 URL 默认过期时间必须大于 0"
		}
	}
}
