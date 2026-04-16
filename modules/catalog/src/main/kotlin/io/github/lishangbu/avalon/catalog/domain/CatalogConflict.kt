package io.github.lishangbu.avalon.catalog.domain

import io.github.lishangbu.avalon.shared.kernel.domain.DomainRuleViolation

/**
 * 用于表达 Catalog 写模型上的唯一性或约束冲突。
 *
 * @param message 面向调用链的冲突说明。
 */
class CatalogConflict(
    message: String,
) : DomainRuleViolation(message)