package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_STAT_CHARACTERISTICS_TABLE = GameDataTableSpec(
	tableName = "game_stat_characteristic",
	label = "数值项特征",
	columns = listOf(
		GameDataColumnSpec(name = "stat_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "characteristic_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("stat_id", "characteristic_id"),
)

/**
 * 数值项特征 Service。
 */
@Service
class GameStatCharacteristicsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_STAT_CHARACTERISTICS_TABLE)
