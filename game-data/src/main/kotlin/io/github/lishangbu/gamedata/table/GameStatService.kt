package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

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
 * 数值项 Service。
 */
@Service
class GameStatService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_STAT_TABLE)
