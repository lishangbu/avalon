package io.github.lishangbu.match.challenge

import org.springframework.http.HttpStatus

/** Challenge 命令可预期失败，code 供前端稳定分支处理。 */
class ChallengeRequestException(val status: HttpStatus, val code: String) : RuntimeException(code)
