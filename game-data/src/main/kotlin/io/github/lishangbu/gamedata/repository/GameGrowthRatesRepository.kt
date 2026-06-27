package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_GROWTH_RATES_TABLE = GameDataTableSpec(
	tableName = "game_growth_rate",
	label = "成长速率",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "formula", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "description", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name", "description"),
)

/**
 * 成长速率持久化访问。
 */
@Repository
class GameGrowthRatesRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_GROWTH_RATES_TABLE)
