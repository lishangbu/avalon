package io.github.lishangbu.avalon.shared.infra.sql.pagination

/**
 * PostgreSQL `LIMIT / OFFSET` 分页语法方言。
 *
 * Vert.x PostgreSQL Client 使用 `$1`、`$2` 这类 1-based 占位符。
 * 调用方只需要传入当前查询中分页参数开始的序号，方言会生成连续的 limit 与 offset 占位符。
 */
object PostgresPaginationDialect : SqlPaginationDialect {
    override fun renderLimitOffsetClause(
        limitPlaceholder: String,
        offsetPlaceholder: String,
    ): String {
        require(limitPlaceholder.isNotBlank()) { "limitPlaceholder must not be blank" }
        require(offsetPlaceholder.isNotBlank()) { "offsetPlaceholder must not be blank" }

        return "LIMIT $limitPlaceholder OFFSET $offsetPlaceholder"
    }

    /**
     * 根据 PostgreSQL 参数序号生成 limit/offset 分页语法片段。
     *
     * 例如 `firstParameterIndex = 3` 时返回 `LIMIT $3 OFFSET $4`。
     * 调用方应按相同顺序向 `Tuple` 追加 `PageRequest.size` 与 `PageRequest.offset`。
     *
     * @param firstParameterIndex limit 参数在当前 prepared query 中的 1-based 序号。
     * @return PostgreSQL 分页 SQL 片段。
     */
    fun renderLimitOffsetClause(firstParameterIndex: Int): String {
        require(firstParameterIndex in 1 until Int.MAX_VALUE) {
            "firstParameterIndex must be between 1 and ${Int.MAX_VALUE - 1}"
        }

        return renderLimitOffsetClause(
            limitPlaceholder = postgresParameter(firstParameterIndex),
            offsetPlaceholder = postgresParameter(firstParameterIndex + 1),
        )
    }

    private fun postgresParameter(index: Int): String = "\$$index"
}