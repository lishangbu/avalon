package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.common.web.filterValue
import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import io.github.lishangbu.gamedata.model.GameDataPage
import io.github.lishangbu.gamedata.model.GameDataRecordRequest
import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.dao.DataIntegrityViolationException
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

private val codePattern = Regex("^[a-z0-9][a-z0-9-]{0,79}$")

/**
 * 具体游戏资料仓储的 Jimmer SQL 基类。
 *
 * 每张资料表仍由自己的 Repository 暴露业务入口和事务注解；本基类只封装 Jimmer 连接、白名单 SQL 拼装、
 * 字段归一化和数据库约束异常翻译，避免把这些安全边界复制到几十个仓储里。表名和字段名全部来自具体仓储传入
 * 的 [GameDataTableSpec]，外部请求不能拼接任意 SQL 标识符。
 */
abstract class GameDataJimmerRepository(
	private val sqlClient: KSqlClient,
	private val table: GameDataTableSpec,
) {
	protected fun listRecords(
		page: Int,
		size: Int,
		query: String?,
		filters: Map<String, String> = emptyMap(),
	): GameDataPage<GameDataRecordResponse> {
		validatePage(page, size)
		val filter = searchFilter(query)
		val whereSql = buildWhereSql(table, filter.pattern, filters)
		val totalRowCount = queryOne(
			sql = "select count(*) from ${table.tableName}${whereSql.text}",
			parameters = whereSql.parameters,
		) { resultSet -> resultSet.getLong(1) } ?: 0L
		val rows = query(
			sql = "select ${selectColumns(table)} from ${table.tableName}${whereSql.text} order by id asc limit ? offset ?",
			parameters = whereSql.parameters + listOf(size, page * size),
		) { resultSet -> resultSet.toRecord(table) }
		return GameDataPage(
			rows = rows,
			totalRowCount = totalRowCount,
			totalPageCount = totalPageCount(totalRowCount, size),
			page = page,
			size = size,
		)
	}

	protected fun getRecord(id: Long): GameDataRecordResponse =
		find(table, id) ?: notFound("id", "${table.label}不存在: $id")

	protected fun createRecord(request: GameDataRecordRequest): GameDataRecordResponse {
		val values = normalizeWriteValues(table, request.fields, requireAll = true)
		if (values.isEmpty()) {
			invalidValue("fields", "fields 至少需要提供一个字段")
		}
		val columns = values.keys.toList()
		val id = queryOne(
			sql = "insert into ${table.tableName} (${columns.joinToString()}) values (${columns.joinToString { "?" }}) returning id",
			parameters = values.values.toList(),
		) { resultSet -> resultSet.getLong(1) } ?: invalidValue("id", "${table.label}创建后未返回主键")
		return getRecord(id)
	}

	protected fun updateRecord(id: Long, request: GameDataRecordRequest): GameDataRecordResponse {
		getRecord(id)
		val values = normalizeWriteValues(table, request.fields, requireAll = false)
		if (values.isEmpty()) {
			invalidValue("fields", "fields 至少需要提供一个字段")
		}
		executeUpdate(
			sql = "update ${table.tableName} set ${values.keys.joinToString { "$it = ?" }} where id = ?",
			parameters = values.values.toList() + id,
		)
		return getRecord(id)
	}

	protected fun deleteRecord(id: Long) {
		val affectedRows = executeUpdate(
			sql = "delete from ${table.tableName} where id = ?",
			parameters = listOf(id),
		)
		if (affectedRows == 0) {
			notFound("id", "${table.label}不存在: $id")
		}
	}

	private fun find(table: GameDataTableSpec, id: Long): GameDataRecordResponse? =
		query(
			sql = "select ${selectColumns(table)} from ${table.tableName} where id = ?",
			parameters = listOf(id),
		) { resultSet -> resultSet.toRecord(table) }.firstOrNull()

	private fun buildWhereSql(
		table: GameDataTableSpec,
		pattern: String?,
		filters: Map<String, String>,
	): SqlFragment {
		val parameters = mutableListOf<Any?>()
		val conditions = buildList {
			if (pattern != null && table.searchColumns.isNotEmpty()) {
				add(
					table.searchColumns.joinToString(prefix = "(", postfix = ")", separator = " or ") { column ->
						parameters += pattern
						"lower(cast($column as text)) like ?"
					},
				)
			}
			addAll(buildExactFilterSql(table, filters, parameters))
		}
		if (conditions.isEmpty()) {
			return SqlFragment("", emptyList())
		}
		return SqlFragment(conditions.joinToString(prefix = " where ", separator = " and "), parameters)
	}

	private fun buildExactFilterSql(
		table: GameDataTableSpec,
		filters: Map<String, String>,
		parameters: MutableList<Any?>,
	): List<String> {
		val columnsByName = table.columns.associateBy(GameDataColumnSpec::name)
		return filters.entries.mapNotNull { (fieldName, rawValue) ->
			val column = columnsByName[fieldName] ?: invalidValue(fieldName, "筛选字段不存在: $fieldName")
			val value = normalizeFilterColumnValue(column, rawValue) ?: return@mapNotNull null
			parameters += value
			"${column.name} = ?"
		}
	}

	private fun normalizeFilterColumnValue(column: GameDataColumnSpec, rawValue: String): Any? {
		val value = filterValue(column.name, rawValue) ?: return null
		return when (column.type) {
			GameDataColumnType.STRING -> value
			GameDataColumnType.LONG -> normalizeLong(column, value, column.name)
			GameDataColumnType.INT -> normalizeInt(column, value, column.name)
			GameDataColumnType.BOOLEAN -> normalizeBoolean(column, value, column.name)
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
			invalidValue(fieldName, "code 只能包含小写字母、数字和连字符，长度为 1 到 80")
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

	/**
	 * 通过 Jimmer 当前连接执行一段白名单 SQL。
	 *
	 * 这里故意只接受位置参数，不提供字符串拼接参数入口；调用方只能把已经由 [GameDataTableSpec] 校验过的
	 * 标识符放进 SQL 文本，所有用户输入都必须进入 [parameters]，从而保留原先命名参数模板的安全边界。
	 */
	private fun <T> query(sql: String, parameters: List<Any?> = emptyList(), mapper: (ResultSet) -> T): List<T> =
		translateSqlException {
			withConnection { connection ->
				connection.prepareStatement(sql).use { statement ->
					statement.bind(parameters)
					statement.executeQuery().use { resultSet ->
						buildList {
							while (resultSet.next()) {
								add(mapper(resultSet))
							}
						}
					}
				}
			}
		}

	private fun <T> queryOne(sql: String, parameters: List<Any?> = emptyList(), mapper: (ResultSet) -> T): T? =
		query(sql, parameters, mapper).singleOrNull()

	private fun executeUpdate(sql: String, parameters: List<Any?> = emptyList()): Int =
		translateSqlException {
			withConnection { connection ->
				connection.prepareStatement(sql).use { statement ->
					statement.bind(parameters)
					statement.executeUpdate()
				}
			}
		}

	private fun <T> withConnection(block: (Connection) -> T): T =
		sqlClient.javaClient.connectionManager.execute { connection -> block(connection) }

	/**
	 * Jimmer 的 `ConnectionManager` 只负责把当前事务连接交给我们，手写 SQL 不再经过 Spring JDBC 模板，
	 * 所以数据库约束错误不会自动翻译成 [DataIntegrityViolationException]。PostgreSQL 把唯一键、外键、
	 * 非空和检查约束都归在 SQLSTATE `23` 这一类；只翻译这一类，可以保留 API 原有的 409 冲突语义，
	 * 又不会把 SQL 语法错误、连接错误等真正的服务端问题误报成业务冲突。
	 */
	private fun <T> translateSqlException(block: () -> T): T =
		try {
			block()
		} catch (exception: SQLException) {
			if (exception.sqlState?.startsWith("23") == true) {
				throw DataIntegrityViolationException("游戏资料数据库约束校验失败", exception)
			}
			throw exception
		}

	private fun PreparedStatement.bind(parameters: List<Any?>) {
		parameters.forEachIndexed { index, value ->
			setObject(index + 1, value)
		}
	}

	private data class SqlFragment(
		val text: String,
		val parameters: List<Any?>,
	)
}
