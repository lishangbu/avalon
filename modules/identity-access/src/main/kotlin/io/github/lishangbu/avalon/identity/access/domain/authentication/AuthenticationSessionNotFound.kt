package io.github.lishangbu.avalon.identity.access.domain.authentication

import io.github.lishangbu.avalon.shared.kernel.domain.DomainRuleViolation

/**
 * 用于表达目标会话不存在或不可见。
 *
 * @param sessionId 不可见或不存在的会话标识。
 */
class AuthenticationSessionNotFound(
    sessionId: String,
) : DomainRuleViolation("session not found: $sessionId")