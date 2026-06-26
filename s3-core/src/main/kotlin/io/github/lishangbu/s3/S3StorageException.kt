package io.github.lishangbu.s3

/**
 * S3 starter 对外暴露的基础异常。
 *
 * 诊断字段只记录 action、object key、HTTP 状态码、S3 错误码和 request id。
 * 异常消息与字段都不保存 AK/SK、签名、endpoint 等敏感配置。
 */
open class S3StorageException(
	message: String,
	cause: Throwable? = null,
	val action: String? = null,
	val key: S3ObjectKey? = null,
	val statusCode: Int? = null,
	val awsErrorCode: String? = null,
	val requestId: String? = null,
) : RuntimeException(message, cause)
