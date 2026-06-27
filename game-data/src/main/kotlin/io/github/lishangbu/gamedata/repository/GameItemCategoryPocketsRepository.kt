package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_ITEM_CATEGORY_POCKETS_TABLE = GameDataTableSpec(
	tableName = "game_item_category_pocket",
	label = "道具分类口袋",
	columns = listOf(
		GameDataColumnSpec(name = "category_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "pocket_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("category_id", "pocket_id"),
)

/**
 * 道具分类口袋持久化访问。
 */
@Repository
class GameItemCategoryPocketsRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_ITEM_CATEGORY_POCKETS_TABLE)
