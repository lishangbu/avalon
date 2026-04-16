package io.github.lishangbu.avalon.identity.access.domain.iam

import io.github.lishangbu.avalon.shared.kernel.domain.DomainRuleViolation

/**
 * 用于表达 IAM 写模型上的唯一性或状态冲突。
 *
 * @param message 面向调用链的冲突说明。
 */
class IdentityAccessConflict(
    message: String,
) : DomainRuleViolation(message)