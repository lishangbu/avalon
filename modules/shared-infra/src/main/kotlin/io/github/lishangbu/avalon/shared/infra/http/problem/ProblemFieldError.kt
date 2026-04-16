package io.github.lishangbu.avalon.shared.infra.http.problem

/**
 * Problem Details 中的字段级错误扩展信息。
 *
 * @property field 触发校验失败的字段名或参数名。
 * @property reason 该字段对应的失败原因。
 */
data class ProblemFieldError(
    val field: String,
    val reason: String,
)