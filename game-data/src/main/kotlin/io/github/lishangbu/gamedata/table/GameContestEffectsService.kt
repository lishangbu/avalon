package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_CONTEST_EFFECTS_TABLE = GameDataTableSpec(
	tableName = "game_contest_effect",
	label = "评价效果",
	columns = listOf(
		GameDataColumnSpec(name = "appeal", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "jam", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "effect", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "flavor_text", type = GameDataColumnType.STRING),
	),
	searchColumns = listOf("effect", "flavor_text"),
)

/**
 * 评价效果 Service。
 */
@Service
class GameContestEffectsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_CONTEST_EFFECTS_TABLE)
