package io.github.lishangbu.match.trainer

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

/** 将公开查找的“不存在”和认证失败稳定映射为玩家 API 错误契约。 */
@RestControllerAdvice(assignableTypes = [PublicTrainerController::class])
class PublicTrainerExceptionHandler {
	@ExceptionHandler(PublicTrainerNotFoundException::class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	fun notFound() = PublicTrainerErrorResponse("public-trainer.not-found", "Public Trainer 不存在")

	@ExceptionHandler(InvalidTrainerSessionException::class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	fun invalidSession() = PublicTrainerErrorResponse("trainer-session.invalid", "Trainer Session 无效或已过期")
}
