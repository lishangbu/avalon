package io.github.lishangbu.avalon.shared.infra.sql.pagination

/**
 * SQL 分页语法方言。
 *
 * 该契约只负责渲染数据库分页语法，不负责业务过滤、排序白名单、`COUNT(*)` 查询或行映射。
 * 调用方仍应在拥有者 bounded context 的 infrastructure 层组织具体 SQL，
 * 并只把可信的占位符传入这里，避免把客户端输入拼接进 SQL 片段。
 */
interface SqlPaginationDialect {
    /**
     * 渲染 limit/offset 分页语法片段。
     *
     * @param limitPlaceholder 单页记录数占位符，例如 PostgreSQL 的 `$3`。
     * @param offsetPlaceholder 0-based 偏移量占位符，例如 PostgreSQL 的 `$4`。
     * @return 可追加在 `ORDER BY` 之后的分页 SQL 片段。
     */
    fun renderLimitOffsetClause(
        limitPlaceholder: String,
        offsetPlaceholder: String,
    ): String
}