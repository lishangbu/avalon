package io.github.lishangbu.s3

/**
 * S3 服务端或 SDK 调用失败时抛出的通用运行时异常。
 *
 * 该异常适合记录到日志或监控中。调用方可以使用 [statusCode]、
 * [awsErrorCode] 和 [requestId] 与对象存储服务侧日志做关联排查。
 */
class S3ServiceException(
	action: String,
	key: S3ObjectKey? = null,
	cause: Throwable? = null,
	statusCode: Int? = null,
	awsErrorCode: String? = null,
	requestId: String? = null,
) : S3StorageException(
	message = serviceFailureMessage(action, key),
	cause = cause,
	action = action,
	key = key,
	statusCode = statusCode,
	awsErrorCode = awsErrorCode,
	requestId = requestId,
)

private fun serviceFailureMessage(action: String, key: S3ObjectKey?): String =
	if (key == null) {
		"S3 $action 调用失败"
	} else {
		"S3 $action 调用失败: ${key.value}"
	}
