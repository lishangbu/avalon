package io.github.lishangbu.common.web

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * 将后台 API 异常转换为稳定 JSON 错误响应。
 */
@RestControllerAdvice(basePackages = ["io.github.lishangbu"])
class ApiExceptionHandler {
	@ExceptionHandler(ApiException::class)
	fun handleApiException(exception: ApiException): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.status(exception.status)
			.body(
				ApiErrorResponse(
					code = exception.code.value,
					message = exception.message,
					field = exception.field,
				),
			)

	@ExceptionHandler(DataIntegrityViolationException::class)
	fun handleDataIntegrityViolation(): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.status(HttpStatus.CONFLICT)
			.body(
				ApiErrorResponse(
					code = ApiErrorCode.RESOURCE_CONFLICT.value,
					message = "当前数据仍被其他资源引用，无法完成操作",
				),
			)

	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun handleMalformedRequest(): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(
				ApiErrorResponse(
					code = ApiErrorCode.MALFORMED_REQUEST.value,
					message = "请求 JSON 无法解析",
				),
			)

	@ExceptionHandler(MethodArgumentTypeMismatchException::class)
	fun handleTypeMismatch(exception: MethodArgumentTypeMismatchException): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(
				ApiErrorResponse(
					code = ApiErrorCode.VALIDATION_INVALID.value,
					message = "${exception.name} 参数格式不正确",
					field = exception.name,
				),
			)
}
