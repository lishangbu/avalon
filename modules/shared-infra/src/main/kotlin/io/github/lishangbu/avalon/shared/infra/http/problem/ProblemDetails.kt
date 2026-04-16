package io.github.lishangbu.avalon.shared.infra.http.problem

/**
 * RFC 9457 `application/problem+json` 媒体类型。
 */
const val PROBLEM_JSON_MEDIA_TYPE: String = "application/problem+json"

/**
 * 构造统一的 Problem Details 响应对象。
 *
 * 若 `type` 传入的是仓库内约定的短键，例如 `catalog/bad-request`，
 * 这里会自动扩展为稳定的 `urn:avalon:problem:*` URI。
 *
 * @param type 问题类型 URI 或短键。
 * @param title 问题标题。
 * @param status HTTP 状态码。
 * @param detail 具体错误说明。
 * @param code 业务错误码扩展字段。
 * @param errors 可选的字段级错误列表。
 * @return 可直接序列化为 RFC 9457 响应体的对象。
 */
fun problemDetails(
    type: String,
    title: String,
    status: Int,
    detail: String,
    code: String,
    errors: List<ProblemFieldError> = emptyList(),
): ProblemDetailsResponse =
    ProblemDetailsResponse(
        type = normalizeProblemType(type),
        title = title,
        status = status,
        detail = detail,
        code = code,
        errors = errors,
    )

private fun normalizeProblemType(type: String): String {
    if (type.startsWith("urn:") || type.startsWith("http://") || type.startsWith("https://")) {
        return type
    }
    return "urn:avalon:problem:${type.replace('/', ':')}"
}