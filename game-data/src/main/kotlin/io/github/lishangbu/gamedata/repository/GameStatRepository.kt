package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_STAT_TABLE = GameDataTableSpec(
	tableName = "game_stat",
	label = "数值项",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "battle_only", type = GameDataColumnType.BOOLEAN),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 数值项持久化访问。
 */
@Repository
class GameStatRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_STAT_TABLE)
