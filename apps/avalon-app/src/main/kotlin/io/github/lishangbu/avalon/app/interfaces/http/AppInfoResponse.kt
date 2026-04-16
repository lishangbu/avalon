package io.github.lishangbu.avalon.app.interfaces.http

import java.time.Instant

/**
 * 应用基础信息响应。
 *
 * @property name 应用名称。
 * @property architecture 当前架构形态标识。
 * @property persistence 当前持久化技术基线标识。
 * @property contexts 当前已注册的主业务上下文列表。
 * @property generatedAt 本次响应生成时间。
 */
data class AppInfoResponse(
    val name: String,
    val architecture: String,
    val persistence: String,
    val contexts: List<String>,
    val generatedAt: String,
)

/**
 * 把当前生成时间映射为应用基础信息响应。
 *
 * 这里把对外暴露的静态架构标识固定在响应层，避免 Resource 重复承载传输对象的组装细节。
 *
 * @return 可直接返回给客户端的应用基础信息。
 */
internal fun Instant.toAppInfoResponse(): AppInfoResponse =
    AppInfoResponse(
        name = "avalon",
        architecture = "modular-monolith",
        persistence = "reactive-sql-client-coroutines",
        contexts = listOf("identity-access", "catalog", "player", "battle"),
        generatedAt = toString(),
    )