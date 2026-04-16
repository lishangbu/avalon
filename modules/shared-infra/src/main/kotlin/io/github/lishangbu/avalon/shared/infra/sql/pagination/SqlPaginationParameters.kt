package io.github.lishangbu.avalon.shared.infra.sql.pagination

import io.github.lishangbu.avalon.shared.application.query.PageRequest
import io.vertx.mutiny.sqlclient.Tuple

/**
 * 按 `LIMIT` 在前、`OFFSET` 在后的顺序追加分页参数。
 *
 * 该顺序匹配 PostgreSQL 与 MySQL 风格的 `LIMIT ? OFFSET ?` 语法。
 * 如果后续引入 `OFFSET` 在前的数据库方言，应新增对应的显式参数追加函数，
 * 不要复用这个函数来隐藏不同数据库之间的绑定顺序差异。
 *
 * @param request 应用层分页请求。
 * @return 追加分页参数后的 `Tuple` 本身。
 */
fun Tuple.addLimitThenOffset(request: PageRequest): Tuple =
    addInteger(request.size)
        .addValue(request.offset)