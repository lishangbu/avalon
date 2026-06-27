package io.github.lishangbu.common.web

import org.springframework.http.HttpStatus

/**
 * 后台 API 的业务错误。
 */
class ApiException(
	val status: HttpStatus,
	val code: ApiErrorCode,
	override val message: String,
	val field: String? = null,
) : RuntimeException(message)
