package io.github.lishangbu.avalon.shared.application.query

/**
 * 应用查询用例使用的 offset 分页请求。
 *
 * 页码面向调用方保持 1-based 语义，避免把数据库 `OFFSET` 的 0-based 细节泄露到应用层。
 * `size` 在应用边界内做上限约束，防止列表查询被无界放大；SQL 层只消费这里派生出的 [offset] 和 [size]。
 *
 * @property page 从 1 开始的页码。
 * @property size 单页记录数，范围为 `1..MAX_SIZE`。
 */
data class PageRequest(
    val page: Int = DEFAULT_PAGE,
    val size: Int = DEFAULT_SIZE,
) {
    init {
        require(page >= MIN_PAGE) { "page must be greater than or equal to $MIN_PAGE" }
        require(size in MIN_SIZE..MAX_SIZE) { "size must be between $MIN_SIZE and $MAX_SIZE" }
    }

    /**
     * 面向 SQL `OFFSET` 的 0-based 偏移量。
     *
     * 使用 `Long` 避免极大页码与页大小相乘时发生 `Int` 溢出。
     */
    val offset: Long
        get() = (page.toLong() - 1L) * size.toLong()

    companion object {
        const val MIN_PAGE: Int = 1
        const val MIN_SIZE: Int = 1
        const val DEFAULT_PAGE: Int = 1
        const val DEFAULT_SIZE: Int = 20
        const val MAX_SIZE: Int = 100
    }
}