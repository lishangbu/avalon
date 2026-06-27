package io.github.lishangbu.common.web

/**
 * 后台 API 对外稳定的错误 code。
 */
enum class ApiErrorCode(val value: String) {
	VALIDATION_REQUIRED("validation.required"),
	VALIDATION_INVALID("validation.invalid"),
	VALIDATION_UNSUPPORTED("validation.unsupported"),
	RESOURCE_CONFLICT("resource.conflict"),
	RESOURCE_NOT_FOUND("resource.not_found"),
	MALFORMED_REQUEST("request.malformed"),
}
