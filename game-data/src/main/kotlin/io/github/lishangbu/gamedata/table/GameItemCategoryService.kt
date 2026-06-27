package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_ITEM_CATEGORY_TABLE = GameDataTableSpec(
	tableName = "game_item_category",
	label = "道具分类",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 道具分类 Service。
 */
@Service
class GameItemCategoryService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_ITEM_CATEGORY_TABLE)
