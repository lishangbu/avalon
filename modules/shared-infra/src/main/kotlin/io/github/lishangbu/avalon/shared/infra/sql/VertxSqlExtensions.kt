package io.github.lishangbu.avalon.shared.infra.sql

import io.vertx.mutiny.sqlclient.Row
import io.vertx.mutiny.sqlclient.RowSet
import io.vertx.mutiny.sqlclient.Tuple
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

/**
 * 把 Vert.x `RowSet` 展平成普通列表，便于后续做 `map/groupBy` 等集合操作。
 *
 * @return 当前结果集中的全部行。
 */
fun RowSet<Row>.toRows(): List<Row> = buildList {
    for (row in this@toRows) {
        add(row)
    }
}

/**
 * 返回结果集中的第一行；若结果为空则抛出异常。
 *
 * @return 结果集中的第一行。
 */
fun RowSet<Row>.first(): Row = firstOrNull() ?: error("Expected at least one row")

/**
 * 返回结果集中的第一行；若结果为空则返回 `null`。
 *
 * @return 第一行或 `null`。
 */
fun RowSet<Row>.firstOrNull(): Row? = iterator().asSequence().firstOrNull()

/**
 * 从 `Row` 中读取可为空的 `Long` 列。
 *
 * @param column 列名。
 * @return 列值；若数据库值为 `NULL` 则返回 `null`。
 */
fun Row.getNullableLong(column: String): Long? = getValue(column)?.let { getLong(column) }

/**
 * 从 `Row` 中读取可为空的 `UUID` 列。
 *
 * @param column 列名。
 * @return 列值；若数据库值为 `NULL` 则返回 `null`。
 */
fun Row.getNullableUUID(column: String): UUID? = getValue(column)?.let { getUUID(column) }

/**
 * 从 `Row` 中读取 `OffsetDateTime` 并转换成 `Instant`。
 *
 * @param column 列名。
 * @return 转换后的时间点。
 */
fun Row.instant(column: String): Instant = getOffsetDateTime(column).toInstant()

/**
 * 在多个候选列名中读取第一个可用的 `UUID` 值。
 *
 * @param columns 候选列名列表。
 * @return 读取到的值。
 */
fun Row.uuidValue(vararg columns: String): UUID =
    columns.firstNotNullOfOrNull { column ->
        runCatching { getUUID(column) }.getOrNull()
    } ?: error("Expected one of columns ${columns.joinToString()} to exist.")

/**
 * 在多个候选列名中读取第一个可用的 `String` 值。
 *
 * @param columns 候选列名列表。
 * @return 读取到的值。
 */
fun Row.stringValue(vararg columns: String): String =
    columns.firstNotNullOfOrNull { column ->
        runCatching { getString(column) }.getOrNull()
    } ?: error("Expected one of columns ${columns.joinToString()} to exist.")

/**
 * 向 `Tuple` 追加 UTC `Instant`，统一数据库写入时区语义。
 *
 * @param instant 待写入的时间点。
 * @return 追加后的 `Tuple` 本身。
 */
fun Tuple.addInstant(instant: Instant): Tuple = addOffsetDateTime(instant.atOffset(ZoneOffset.UTC))
