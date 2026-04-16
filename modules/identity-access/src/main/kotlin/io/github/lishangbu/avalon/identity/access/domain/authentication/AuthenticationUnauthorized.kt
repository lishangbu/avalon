package io.github.lishangbu.avalon.identity.access.domain.authentication

import io.github.lishangbu.avalon.shared.kernel.domain.DomainRuleViolation

/**
 * 用于表达当前请求缺少有效认证主体。
 *
 * @param message 未授权说明。
 */
class AuthenticationUnauthorized(
    message: String = "Unauthorized.",
) : DomainRuleViolation(message)