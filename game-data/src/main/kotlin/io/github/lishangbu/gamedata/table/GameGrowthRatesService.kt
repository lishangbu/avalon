package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

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
 * 成长速率 Service。
 */
@Service
class GameGrowthRatesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_GROWTH_RATES_TABLE)
