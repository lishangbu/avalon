package io.github.lishangbu.avalon.identity.access.domain.authentication

import io.github.lishangbu.avalon.shared.kernel.domain.DomainRuleViolation

/**
 * 用于表达登录、刷新令牌等认证动作失败。
 *
 * @param message 认证失败说明。
 */
class AuthenticationFailed(
    message: String = "Invalid credentials.",
) : DomainRuleViolation(message)