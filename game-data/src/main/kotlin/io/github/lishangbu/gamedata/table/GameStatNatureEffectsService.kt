package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_STAT_NATURE_EFFECTS_TABLE = GameDataTableSpec(
	tableName = "game_stat_nature_effect",
	label = "数值项性格影响",
	columns = listOf(
		GameDataColumnSpec(name = "stat_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "nature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "effect_type", type = GameDataColumnType.STRING, required = true, maxLength = 20),
	),
	searchColumns = listOf("stat_id", "nature_id"),
)

/**
 * 数值项性格影响 Service。
 */
@Service
class GameStatNatureEffectsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_STAT_NATURE_EFFECTS_TABLE)
