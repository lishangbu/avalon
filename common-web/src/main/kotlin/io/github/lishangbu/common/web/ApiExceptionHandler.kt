package io.github.lishangbu.common.web

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.sql.SQLException

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

	/**
	 * 将持久化框架保留在异常链中的数据库完整性约束错误转换为资源冲突。
	 *
	 * SQLState `23` 是标准完整性约束类别，覆盖唯一键和外键冲突；其他 SQL 异常继续抛出，
	 * 避免把连接中断、语法错误等服务端故障伪装成可由客户端修正的 409 响应。
	 */
	@ExceptionHandler(SQLException::class)
	fun handleDatabaseConstraintViolation(exception: SQLException): ResponseEntity<ApiErrorResponse> {
		if (!exception.sqlState.orEmpty().startsWith(INTEGRITY_CONSTRAINT_SQL_STATE_CLASS)) {
			throw exception
		}
		return handleDataIntegrityViolation()
	}

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

	private companion object {
		private const val INTEGRITY_CONSTRAINT_SQL_STATE_CLASS = "23"
	}
}
