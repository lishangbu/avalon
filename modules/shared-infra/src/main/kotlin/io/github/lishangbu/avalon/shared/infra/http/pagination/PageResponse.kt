package io.github.lishangbu.avalon.shared.infra.http.pagination

import io.github.lishangbu.avalon.shared.application.query.Page

/**
 * HTTP 列表接口统一返回的 offset 分页响应。
 *
 * 该类型只承载接口层对外暴露的分页形状，便于多个 bounded context 复用一致的
 * JSON 结构；真正的分页校验、筛选语义和 SQL 方言仍由各自上下文决定。
 *
 * @param T 当前页元素类型。
 * @property items 当前页命中的元素列表。
 * @property page 当前页码，沿用应用层 `1-based` 语义。
 * @property size 单页记录数。
 * @property totalItems 符合当前查询条件的总记录数。
 * @property totalPages 总页数；当总记录数为 `0` 时返回 `0`。
 * @property hasNext 当前页之后是否仍有下一页；虽然 offset 分页可由 `page` 与
 * `totalPages` 推导，但这里保留显式字段，便于调用方统一消费“是否还能继续翻页”的语义，
 * 并减少未来分页策略调整时的响应形状波动。
 */
data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Long,
    val hasNext: Boolean,
)

/**
 * 将应用层分页结果转换为 HTTP 分页响应，同时转换当前页元素。
 *
 * @param transform 元素转换函数。
 * @return 可直接返回给调用方的分页响应。
 */
fun <T, R> Page<T>.toResponse(transform: (T) -> R): PageResponse<R> =
    PageResponse(
        items = items.map(transform),
        page = page,
        size = size,
        totalItems = totalItems,
        totalPages = totalPages,
        hasNext = hasNext,
    )