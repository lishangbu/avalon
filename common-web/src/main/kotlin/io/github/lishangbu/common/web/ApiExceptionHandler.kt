package io.github.lishangbu.common.web

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.server.ResponseStatusException
import org.slf4j.LoggerFactory
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
	fun handleDataIntegrityViolation(exception: DataIntegrityViolationException): ResponseEntity<ApiErrorResponse> =
		handleIntegritySqlState(findSqlException(exception)?.sqlState)

	private fun handleIntegritySqlState(sqlState: String?): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.status(HttpStatus.CONFLICT)
			.body(
				ApiErrorResponse(
					code = ApiErrorCode.RESOURCE_CONFLICT.value,
					message = when (sqlState) {
						UNIQUE_VIOLATION -> "资源的唯一字段已存在"
						FOREIGN_KEY_VIOLATION -> "当前数据仍被其他资源引用，无法完成操作"
						CHECK_VIOLATION -> "数据不满足数据库约束"
						else -> "数据完整性约束冲突"
					},
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
		return handleIntegritySqlState(exception.sqlState)
	}

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleBeanValidation(exception: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
		val error = exception.bindingResult.fieldErrors.firstOrNull()
		val required = error?.rejectedValue == null || (error.rejectedValue as? String)?.isBlank() == true
		return ResponseEntity.badRequest().body(
			ApiErrorResponse(
				code = if (required) ApiErrorCode.VALIDATION_REQUIRED.value else ApiErrorCode.VALIDATION_INVALID.value,
				message = error?.defaultMessage ?: "请求字段校验失败",
				field = error?.field,
			),
		)
	}

	@ExceptionHandler(HandlerMethodValidationException::class)
	fun handleMethodValidation(): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.badRequest().body(
			ApiErrorResponse(ApiErrorCode.VALIDATION_INVALID.value, "请求参数校验失败"),
		)

	@ExceptionHandler(ResponseStatusException::class)
	fun handleResponseStatus(exception: ResponseStatusException): ResponseEntity<ApiErrorResponse> {
		val status = HttpStatus.resolve(exception.statusCode.value()) ?: HttpStatus.INTERNAL_SERVER_ERROR
		val code = when (status) {
			HttpStatus.UNAUTHORIZED -> "authentication.required"
			HttpStatus.FORBIDDEN -> "authorization.denied"
			HttpStatus.TOO_MANY_REQUESTS -> ApiErrorCode.RATE_LIMITED.value
			else -> if (status.is4xxClientError) "request.rejected" else ApiErrorCode.INTERNAL_ERROR.value
		}
		return ResponseEntity.status(status).body(ApiErrorResponse(code, exception.reason ?: status.reasonPhrase))
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

	@ExceptionHandler(Exception::class)
	fun handleUnknown(exception: Exception): ResponseEntity<ApiErrorResponse> {
		findSqlException(exception)?.sqlState?.takeIf { it.startsWith(INTEGRITY_CONSTRAINT_SQL_STATE_CLASS) }
			?.let { return handleIntegritySqlState(it) }
		logger.error("Unhandled API exception", exception)
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
			ApiErrorResponse(ApiErrorCode.INTERNAL_ERROR.value, "服务器内部错误"),
		)
	}

	private fun findSqlException(exception: Throwable): SQLException? =
		generateSequence(exception as Throwable?) { it.cause }.filterIsInstance<SQLException>().firstOrNull()

	private companion object {
		val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
		private const val INTEGRITY_CONSTRAINT_SQL_STATE_CLASS = "23"
		private const val UNIQUE_VIOLATION = "23505"
		private const val FOREIGN_KEY_VIOLATION = "23503"
		private const val CHECK_VIOLATION = "23514"
	}
}
