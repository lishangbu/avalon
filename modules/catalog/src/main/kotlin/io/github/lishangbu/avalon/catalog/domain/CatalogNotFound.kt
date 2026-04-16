package io.github.lishangbu.avalon.catalog.domain

import io.github.lishangbu.avalon.shared.kernel.domain.DomainRuleViolation

/**
 * 用于表达 Catalog 资源不存在的领域失败。
 *
 * @param resource 不存在的资源类型。
 * @param identifier 资源标识文本。
 */
class CatalogNotFound(
    resource: String,
    identifier: String,
) : DomainRuleViolation("$resource not found: $identifier")