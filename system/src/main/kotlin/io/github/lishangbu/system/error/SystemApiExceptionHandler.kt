package io.github.lishangbu.system.error

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * 将系统管理 API 的异常转换为稳定 JSON 错误响应。
 */
@RestControllerAdvice(basePackages = ["io.github.lishangbu.system"])
class SystemApiExceptionHandler {
	@ExceptionHandler(SystemApiException::class)
	fun handleSystemApiException(exception: SystemApiException): ResponseEntity<SystemApiErrorResponse> =
		ResponseEntity.status(exception.status)
			.body(
				SystemApiErrorResponse(
					code = exception.code.value,
					message = exception.message,
					field = exception.field,
				),
			)

	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun handleMalformedRequest(): ResponseEntity<SystemApiErrorResponse> =
		ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(
				SystemApiErrorResponse(
					code = SystemApiErrorCode.MALFORMED_REQUEST.value,
					message = "请求 JSON 无法解析",
				),
			)

	@ExceptionHandler(MethodArgumentTypeMismatchException::class)
	fun handleTypeMismatch(exception: MethodArgumentTypeMismatchException): ResponseEntity<SystemApiErrorResponse> =
		ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(
				SystemApiErrorResponse(
					code = SystemApiErrorCode.VALIDATION_INVALID.value,
					message = "${exception.name} 参数格式不正确",
					field = exception.name,
				),
			)
}
