package io.github.lishangbu.avalon.identity.access.domain.iam

import io.github.lishangbu.avalon.shared.kernel.domain.DomainRuleViolation

/**
 * 用于表达 Identity Access 上下文自己的坏请求输入。
 *
 * 这类异常应由 Identity Access 的 HTTP 映射器统一翻译为 `400 Bad Request`，
 * 避免被其他上下文的通用异常映射器误接管。
 *
 * @param message 面向调用方的非法输入说明。
 */
class IdentityAccessBadRequest(
    message: String,
) : DomainRuleViolation(message)