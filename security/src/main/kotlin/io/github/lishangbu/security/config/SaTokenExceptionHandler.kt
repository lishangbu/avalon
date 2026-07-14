package io.github.lishangbu.security.config

import cn.dev33.satoken.exception.NotLoginException
import cn.dev33.satoken.exception.NotPermissionException
import cn.dev33.satoken.exception.NotRoleException
import io.github.lishangbu.common.web.ApiErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** 将 Sa-Token 鉴权异常转换为稳定的后台 API 错误响应。 */
@RestControllerAdvice(basePackages = ["io.github.lishangbu"])
class SaTokenExceptionHandler {
	@ExceptionHandler(NotLoginException::class)
	fun handleNotLogin(): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
			ApiErrorResponse(
				code = "authentication.required",
				message = "请先登录后再访问",
			),
		)

	@ExceptionHandler(NotPermissionException::class, NotRoleException::class)
	fun handleAccessDenied(): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.status(HttpStatus.FORBIDDEN).body(
			ApiErrorResponse(
				code = "authorization.denied",
				message = "当前账号没有执行此操作的权限",
			),
		)
}
