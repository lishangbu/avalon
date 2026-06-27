package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_ITEM_GAME_INDICES_TABLE = GameDataTableSpec(
	tableName = "game_item_game_index",
	label = "道具索引",
	columns = listOf(
		GameDataColumnSpec(name = "item_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "game_index", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("item_id"),
)

/**
 * 道具索引持久化访问。
 */
@Repository
class GameItemGameIndicesRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_ITEM_GAME_INDICES_TABLE)
