package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_ITEM_POCKETS_TABLE = GameDataTableSpec(
	tableName = "game_item_pocket",
	label = "道具口袋",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 道具口袋持久化访问。
 */
@Repository
class GameItemPocketsRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_ITEM_POCKETS_TABLE)
