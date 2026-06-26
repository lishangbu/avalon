package io.github.lishangbu.s3.autoconfigure

import java.time.Duration

/**
 * S3 预签名 URL 默认配置。
 */
class S3PresignProperties {
	/**
	 * 未显式指定过期时间时使用的预签名 URL TTL。
	 */
	var defaultTtl: Duration = Duration.ofMinutes(15)
}
