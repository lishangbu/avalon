package io.github.lishangbu.s3

/**
 * S3 服务拒绝访问时抛出的异常。
 *
 * 调用方通常应将该异常映射为权限不足或凭证无效，而不是重试。
 */
class S3AccessDeniedException(
	action: String,
	key: S3ObjectKey? = null,
	cause: Throwable? = null,
	statusCode: Int? = null,
	awsErrorCode: String? = null,
	requestId: String? = null,
) : S3StorageException(
	message = accessDeniedMessage(action, key),
	cause = cause,
	action = action,
	key = key,
	statusCode = statusCode,
	awsErrorCode = awsErrorCode,
	requestId = requestId,
)

private fun accessDeniedMessage(action: String, key: S3ObjectKey?): String =
	if (key == null) {
		"S3 $action 被拒绝"
	} else {
		"S3 $action 被拒绝: ${key.value}"
	}
