package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_LOCATION_GAME_INDICES_TABLE = GameDataTableSpec(
	tableName = "game_location_game_index",
	label = "地点索引",
	columns = listOf(
		GameDataColumnSpec(name = "location_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "game_index", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("location_id"),
)

/**
 * 地点索引持久化访问。
 */
@Repository
class GameLocationGameIndicesRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_LOCATION_GAME_INDICES_TABLE)
