package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_ITEM_TABLE = GameDataTableSpec(
	tableName = "game_item",
	label = "道具资料",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "category_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "cost", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "fling_power", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 道具资料持久化访问。
 */
@Repository
class GameItemRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_ITEM_TABLE)
