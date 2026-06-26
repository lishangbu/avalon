package io.github.lishangbu.s3

import java.net.URI
import java.time.Instant

/**
 * 预签名 URL 和调用方需要携带的请求信息。
 */
data class S3PresignedUrl(
	val key: S3ObjectKey,
	val url: URI,
	val method: String,
	val expiresAt: Instant,
	val headers: Map<String, List<String>>,
)
