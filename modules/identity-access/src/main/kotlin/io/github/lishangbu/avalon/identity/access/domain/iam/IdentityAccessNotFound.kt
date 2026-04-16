package io.github.lishangbu.avalon.identity.access.domain.iam

import io.github.lishangbu.avalon.shared.kernel.domain.DomainRuleViolation

/**
 * 用于表达用户、角色、权限或菜单不存在的领域失败。
 *
 * @param resource 不存在的资源类型。
 * @param identifier 资源标识文本。
 */
class IdentityAccessNotFound(
    resource: String,
    identifier: String,
) : DomainRuleViolation("$resource not found: $identifier")