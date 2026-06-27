package io.github.lishangbu.gamedata.repository

/**
 * 单张游戏资料表的 SQL 白名单配置。
 */
data class GameDataTableSpec(
	val tableName: String,
	val label: String,
	val columns: List<GameDataColumnSpec>,
	val searchColumns: List<String> = emptyList(),
) {
	init {
		require(safeIdentifierPattern.matches(tableName)) { "非法表名: $tableName" }
		columns.forEach { column ->
			require(safeIdentifierPattern.matches(column.name)) { "非法字段名: ${column.name}" }
		}
		searchColumns.forEach { column ->
			require(columns.any { it.name == column }) { "搜索字段不存在: $column" }
		}
	}
}

private val safeIdentifierPattern = Regex("^[a-z][a-z0-9_]*$")
