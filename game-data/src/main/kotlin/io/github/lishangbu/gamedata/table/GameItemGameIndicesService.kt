package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_ITEM_GAME_INDICES_TABLE = GameDataTableSpec(
	tableName = "game_item_game_index",
	label = "道具版本索引",
	columns = listOf(
		GameDataColumnSpec(name = "item_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "generation_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "game_index", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("item_id", "generation_id"),
)

/**
 * 道具版本索引 Service。
 */
@Service
class GameItemGameIndicesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_ITEM_GAME_INDICES_TABLE)
