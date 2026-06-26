package io.github.lishangbu.s3

/**
 * 指定 object 不存在时抛出的异常。
 *
 * 该异常表示目标 object 确认不存在；调用方可以按业务需要返回空结果、
 * 404 响应或触发补偿上传。
 */
class S3ObjectNotFoundException(
	key: S3ObjectKey,
	cause: Throwable? = null,
	action: String? = null,
	statusCode: Int? = null,
	awsErrorCode: String? = null,
	requestId: String? = null,
) : S3StorageException(
	message = "S3 object 不存在: ${key.value}",
	cause = cause,
	action = action,
	key = key,
	statusCode = statusCode,
	awsErrorCode = awsErrorCode,
	requestId = requestId,
)
