package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_ADVANCED_CONTEST_EFFECTS_TABLE = GameDataTableSpec(
	tableName = "game_advanced_contest_effect",
	label = "高级评价效果",
	columns = listOf(
		GameDataColumnSpec(name = "appeal", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "flavor_text", type = GameDataColumnType.STRING),
	),
	searchColumns = listOf("flavor_text"),
)

/**
 * 高级评价效果 Service。
 */
@Service
class GameAdvancedContestEffectsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_ADVANCED_CONTEST_EFFECTS_TABLE)
