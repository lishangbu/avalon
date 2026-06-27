package io.github.lishangbu.gamedata.table

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.api.GameDataPageResponse
import io.github.lishangbu.gamedata.api.GameDataRecordRequest
import io.github.lishangbu.gamedata.api.GameDataRecordResponse
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet

private val codePattern = Regex("^[a-z0-9][a-z0-9-]{1,79}$")

/**
 * 游戏资料通用 JDBC CRUD 操作。
 *
 * ponytail: 每张表保留独立 Controller/Service，SQL 只保留一份白名单实现，避免复制十几套相同分页和校验逻辑。
 */
@Component
class GameDataJdbcOperations(
	private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
	@Transactional(readOnly = true)
	fun list(table: GameDataTableSpec, page: Int, size: Int, query: String?): GameDataPageResponse {
		validatePage(page, size)
		val filter = searchFilter(query)
		val params = MapSqlParameterSource()
		val whereSql = buildWhereSql(table, filter.pattern, params)
		val totalRowCount = jdbcTemplate.queryForObject(
			"select count(*) from ${table.tableName}$whereSql",
			params,
			Long::class.java,
		) ?: 0L
		params.addValue("limit", size)
		params.addValue("offset", page * size)
		val rows = jdbcTemplate.query(
			"select ${selectColumns(table)} from ${table.tableName}$whereSql order by id asc limit :limit offset :offset",
			params,
		) { resultSet, _ -> resultSet.toRecord(table) }
		return GameDataPageResponse(
			rows = rows,
			totalRowCount = totalRowCount,
			totalPageCount = totalPageCount(totalRowCount, size),
			page = page,
			size = size,
		)
	}

	@Transactional(readOnly = true)
	fun get(table: GameDataTableSpec, id: Long): GameDataRecordResponse =
		find(table, id) ?: notFound("id", "${table.label}不存在: $id")

	@Transactional
	fun create(table: GameDataTableSpec, request: GameDataRecordRequest): GameDataRecordResponse {
		val values = normalizeWriteValues(table, request.fields, requireAll = true)
		if (values.isEmpty()) {
			invalidValue("fields", "fields 至少需要提供一个字段")
		}
		val columns = values.keys.toList()
		val params = MapSqlParameterSource(values)
		val id = jdbcTemplate.queryForObject(
			"insert into ${table.tableName} (${columns.joinToString()}) values (${columns.joinToString { ":$it" }}) returning id",
			params,
			Long::class.java,
		) ?: invalidValue("id", "${table.label}创建后未返回主键")
		return get(table, id)
	}

	@Transactional
	fun update(table: GameDataTableSpec, id: Long, request: GameDataRecordRequest): GameDataRecordResponse {
		get(table, id)
		val values = normalizeWriteValues(table, request.fields, requireAll = false)
		if (values.isEmpty()) {
			invalidValue("fields", "fields 至少需要提供一个字段")
		}
		val params = MapSqlParameterSource(values).addValue("id", id)
		jdbcTemplate.update(
			"update ${table.tableName} set ${values.keys.joinToString { "$it = :$it" }} where id = :id",
			params,
		)
		return get(table, id)
	}

	@Transactional
	fun delete(table: GameDataTableSpec, id: Long) {
		val affectedRows = jdbcTemplate.update(
			"delete from ${table.tableName} where id = :id",
			MapSqlParameterSource("id", id),
		)
		if (affectedRows == 0) {
			notFound("id", "${table.label}不存在: $id")
		}
	}

	private fun find(table: GameDataTableSpec, id: Long): GameDataRecordResponse? =
		jdbcTemplate.query(
			"select ${selectColumns(table)} from ${table.tableName} where id = :id",
			MapSqlParameterSource("id", id),
		) { resultSet, _ -> resultSet.toRecord(table) }.firstOrNull()

	private fun buildWhereSql(table: GameDataTableSpec, pattern: String?, params: MapSqlParameterSource): String {
		if (pattern == null || table.searchColumns.isEmpty()) {
			return ""
		}
		params.addValue("q", pattern)
		return table.searchColumns.joinToString(prefix = " where (", postfix = ")", separator = " or ") { column ->
			"lower(cast($column as text)) like :q"
		}
	}

	private fun selectColumns(table: GameDataTableSpec): String =
		buildList {
			add("id")
			addAll(table.columns.map { it.name })
		}.joinToString()

	private fun ResultSet.toRecord(table: GameDataTableSpec): GameDataRecordResponse =
		GameDataRecordResponse(
			id = getLong("id"),
			fields = table.columns.associate { column -> column.name to getObject(column.name) },
		)

	private fun normalizeWriteValues(table: GameDataTableSpec, fields: Map<String, Any?>, requireAll: Boolean): Map<String, Any?> {
		val writableColumns = table.columns.filter(GameDataColumnSpec::writable)
		val writableByName = writableColumns.associateBy(GameDataColumnSpec::name)
		val unsupportedFields = fields.keys.filterNot(writableByName::containsKey)
		if (unsupportedFields.isNotEmpty()) {
			invalidValue("fields", "不支持字段: ${unsupportedFields.joinToString()}")
		}
		return writableColumns.mapNotNull { column ->
			if (!fields.containsKey(column.name)) {
				if (requireAll && column.required) {
					invalidValue("fields.${column.name}", "${column.name} 不能为空")
				}
				null
			} else {
				column.name to normalizeColumnValue(column, fields[column.name])
			}
		}.toMap()
	}

	private fun normalizeColumnValue(column: GameDataColumnSpec, rawValue: Any?): Any? {
		val fieldName = "fields.${column.name}"
		if (rawValue == null) {
			if (column.required) {
				invalidValue(fieldName, "${column.name} 不能为空")
			}
			return null
		}
		return when (column.type) {
			GameDataColumnType.STRING -> normalizeString(column, rawValue, fieldName)
			GameDataColumnType.LONG -> normalizeLong(column, rawValue, fieldName)
			GameDataColumnType.INT -> normalizeInt(column, rawValue, fieldName)
			GameDataColumnType.BOOLEAN -> normalizeBoolean(column, rawValue, fieldName)
		}
	}

	private fun normalizeString(column: GameDataColumnSpec, rawValue: Any, fieldName: String): String? {
		val value = rawValue.toString().trim()
		if (value.isBlank()) {
			if (column.required) {
				invalidValue(fieldName, "${column.name} 不能为空")
			}
			return null
		}
		if (column.name == "code" && !codePattern.matches(value)) {
			invalidValue(fieldName, "code 只能包含小写字母、数字和连字符，长度为 2 到 80")
		}
		val maxLength = column.maxLength
		if (maxLength != null && value.length > maxLength) {
			invalidValue(fieldName, "${column.name} 长度不能超过 $maxLength")
		}
		return value
	}

	private fun normalizeLong(column: GameDataColumnSpec, rawValue: Any, fieldName: String): Long {
		val value = when (rawValue) {
			is Number -> rawValue.toLong()
			is String -> rawValue.trim().toLongOrNull()
			else -> null
		}
		return value ?: invalidValue(fieldName, "${column.name} 必须是整数")
	}

	private fun normalizeInt(column: GameDataColumnSpec, rawValue: Any, fieldName: String): Int {
		val value = when (rawValue) {
			is Number -> rawValue.toInt()
			is String -> rawValue.trim().toIntOrNull()
			else -> null
		}
		return value ?: invalidValue(fieldName, "${column.name} 必须是整数")
	}

	private fun normalizeBoolean(column: GameDataColumnSpec, rawValue: Any, fieldName: String): Boolean {
		val value = when (rawValue) {
			is Boolean -> rawValue
			is String -> when (rawValue.trim().lowercase()) {
				"true" -> true
				"false" -> false
				else -> null
			}
			else -> null
		}
		return value ?: invalidValue(fieldName, "${column.name} 必须是布尔值")
	}

	private fun totalPageCount(totalRowCount: Long, size: Int): Int {
		if (totalRowCount == 0L) {
			return 0
		}
		return ((totalRowCount + size - 1) / size).toInt()
	}
}
