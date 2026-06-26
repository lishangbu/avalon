package io.github.lishangbu.system.error

import org.springframework.http.HttpStatus

/**
 * 系统管理 API 的业务错误。
 */
class SystemApiException(
	val status: HttpStatus,
	val code: SystemApiErrorCode,
	override val message: String,
	val field: String? = null,
) : RuntimeException(message)
