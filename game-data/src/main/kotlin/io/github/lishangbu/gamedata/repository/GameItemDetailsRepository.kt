package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_ITEM_DETAILS_TABLE = GameDataTableSpec(
	tableName = "game_item_detail",
	label = "道具详情",
	columns = listOf(
		GameDataColumnSpec(name = "item_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "fling_effect_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "effect", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "short_effect", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "flavor_text", type = GameDataColumnType.STRING),
	),
	searchColumns = listOf("item_id", "effect", "flavor_text"),
)

/**
 * 道具详情持久化访问。
 */
@Repository
class GameItemDetailsRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_ITEM_DETAILS_TABLE)
