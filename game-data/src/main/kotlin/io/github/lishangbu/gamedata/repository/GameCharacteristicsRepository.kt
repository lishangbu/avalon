package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_CHARACTERISTICS_TABLE = GameDataTableSpec(
	tableName = "game_characteristic",
	label = "个体特征",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 200),
		GameDataColumnSpec(name = "highest_stat_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "gene_modulo", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "description", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name", "description"),
)

/**
 * 个体特征持久化访问。
 */
@Repository
class GameCharacteristicsRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_CHARACTERISTICS_TABLE)
