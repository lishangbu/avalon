package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_GROWTH_RATE_LEVELS_TABLE = GameDataTableSpec(
	tableName = "game_growth_rate_level",
	label = "成长等级经验",
	columns = listOf(
		GameDataColumnSpec(name = "growth_rate_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "level", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "experience", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("growth_rate_id", "level"),
)

/**
 * 成长等级经验 Service。
 */
@Service
class GameGrowthRateLevelsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_GROWTH_RATE_LEVELS_TABLE)
