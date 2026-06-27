package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_LOCATION_GAME_INDICES_TABLE = GameDataTableSpec(
	tableName = "game_location_game_index",
	label = "地点版本索引",
	columns = listOf(
		GameDataColumnSpec(name = "location_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "generation_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "game_index", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("location_id", "generation_id"),
)

/**
 * 地点版本索引 Service。
 */
@Service
class GameLocationGameIndicesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_LOCATION_GAME_INDICES_TABLE)
