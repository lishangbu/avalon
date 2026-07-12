package io.github.lishangbu.match.trainer

import org.springframework.http.HttpStatus

/** 带稳定错误 code 的 Trainer Session HTTP 边界异常。 */
class TrainerSessionRequestException(val status: HttpStatus, val code: String) : RuntimeException(code)
