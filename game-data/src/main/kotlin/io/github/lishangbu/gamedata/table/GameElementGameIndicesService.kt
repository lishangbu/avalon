package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_ELEMENT_GAME_INDICES_TABLE = GameDataTableSpec(
	tableName = "game_element_game_index",
	label = "属性版本索引",
	columns = listOf(
		GameDataColumnSpec(name = "element_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "generation_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "game_index", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("element_id", "generation_id"),
)

/**
 * 属性版本索引 Service。
 */
@Service
class GameElementGameIndicesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_ELEMENT_GAME_INDICES_TABLE)
