package io.github.lishangbu.match.trainer

/** 前端用于区分 OAuth 失效与 Trainer Session 失效的稳定错误响应。 */
data class TrainerSessionErrorResponse(val code: String, val message: String)
