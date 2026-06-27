package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

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
 * 数值项特征持久化访问。
 */
@Repository
class GameStatCharacteristicsRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_STAT_CHARACTERISTICS_TABLE)
