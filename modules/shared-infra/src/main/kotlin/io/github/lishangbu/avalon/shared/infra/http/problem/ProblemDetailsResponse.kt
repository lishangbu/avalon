package io.github.lishangbu.avalon.shared.infra.http.problem

/**
 * RFC 9457 Problem Details 错误响应。
 *
 * 这里统一的是错误响应的对外形状，而不是具体业务语义。各上下文仍需在自己的
 * exception mapper 中决定 `type`、`detail` 与扩展字段 `code` 的取值。
 *
 * @property type 指向具体错误类别的稳定 URI。
 * @property title 问题标题，通常与 HTTP 状态语义一致。
 * @property status 本次响应的 HTTP 状态码。
 * @property detail 面向调用方的具体错误说明。
 * @property code 面向前端与调用方的稳定业务错误码扩展字段。
 * @property errors 可选的字段级错误列表，主要用于校验失败场景。
 */
data class ProblemDetailsResponse(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String,
    val code: String,
    val errors: List<ProblemFieldError> = emptyList(),
)