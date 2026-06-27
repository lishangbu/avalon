package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_ITEM_FLING_EFFECTS_TABLE = GameDataTableSpec(
	tableName = "game_item_fling_effect",
	label = "道具投掷效果",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "effect", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name", "effect"),
)

/**
 * 道具投掷效果 Service。
 */
@Service
class GameItemFlingEffectsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_ITEM_FLING_EFFECTS_TABLE)
