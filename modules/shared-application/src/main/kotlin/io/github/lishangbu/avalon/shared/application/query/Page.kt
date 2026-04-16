package io.github.lishangbu.avalon.shared.application.query

/**
 * 应用查询用例返回的 offset 分页结果。
 *
 * 该类型只表达应用层分页元数据，不绑定 HTTP 响应信封、数据库 `LIMIT/OFFSET` 语法或具体资源的筛选排序语义。
 * 调用方可以通过 [map] 在保留分页信息的同时转换列表元素，避免接口层重复组装分页元数据。
 *
 * @param T 分页内的元素类型。
 * @property items 当前页命中的元素；页码超出数据范围时可以为空。
 * @property page 当前页码，沿用 [PageRequest] 的 1-based 语义。
 * @property size 单页记录数。
 * @property totalItems 符合当前查询条件的总记录数。
 */
data class Page<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
) {
    init {
        require(page >= PageRequest.MIN_PAGE) { "page must be greater than or equal to ${PageRequest.MIN_PAGE}" }
        require(size in PageRequest.MIN_SIZE..PageRequest.MAX_SIZE) {
            "size must be between ${PageRequest.MIN_SIZE} and ${PageRequest.MAX_SIZE}"
        }
        require(totalItems >= 0L) { "totalItems must be greater than or equal to 0" }
    }

    /**
     * 当前查询条件下的总页数。
     *
     * 无命中数据时返回 `0`，这样调用方可以明确区分“第一页但没有数据”和“存在至少一页数据”。
     */
    val totalPages: Long
        get() = if (totalItems == 0L) 0L else (totalItems + size - 1L) / size

    /**
     * 当前页之后是否仍有下一页。
     *
     * 对 offset 分页来说，这个值理论上可以由 `page < totalPages` 推导；
     * 这里仍然保留显式语义，是为了让接口层直接暴露“是否还有下一页”的稳定概念，
     * 并在后续部分列表切到 cursor 分页时，尽量保持调用方围绕同一翻页信号处理导航逻辑。
     */
    val hasNext: Boolean
        get() = page.toLong() < totalPages

    /**
     * 转换当前页元素，同时保留分页元数据。
     *
     * @param transform 元素转换函数。
     * @return 元素类型转换后的分页结果。
     */
    fun <R> map(transform: (T) -> R): Page<R> =
        Page(
            items = items.map(transform),
            page = page,
            size = size,
            totalItems = totalItems,
        )

    companion object {
        /**
         * 根据分页请求创建结果，避免调用方重复拆解 [PageRequest]。
         *
         * @param items 当前页命中的元素。
         * @param request 本次查询使用的分页请求。
         * @param totalItems 符合当前查询条件的总记录数。
         * @return 分页结果。
         */
        fun <T> of(
            items: List<T>,
            request: PageRequest,
            totalItems: Long,
        ): Page<T> =
            Page(
                items = items,
                page = request.page,
                size = request.size,
                totalItems = totalItems,
            )
    }
}