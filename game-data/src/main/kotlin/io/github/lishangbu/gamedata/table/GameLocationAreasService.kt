package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_LOCATION_AREAS_TABLE = GameDataTableSpec(
	tableName = "game_location_area",
	label = "地点区域",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "location_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "game_index", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 地点区域 Service。
 */
@Service
class GameLocationAreasService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_LOCATION_AREAS_TABLE)
