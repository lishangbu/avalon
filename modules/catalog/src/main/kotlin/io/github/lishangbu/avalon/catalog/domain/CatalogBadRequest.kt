package io.github.lishangbu.avalon.catalog.domain

import io.github.lishangbu.avalon.shared.kernel.domain.DomainRuleViolation

/**
 * 用于表达 Catalog 上下文自己的坏请求输入。
 *
 * 这类异常应由 Catalog 的 HTTP 映射器统一翻译为 `400 Bad Request`，
 * 避免退化成别的上下文的通用 `IllegalArgumentException`。
 *
 * @param message 面向调用方的非法输入说明。
 */
class CatalogBadRequest(
    message: String,
) : DomainRuleViolation(message)