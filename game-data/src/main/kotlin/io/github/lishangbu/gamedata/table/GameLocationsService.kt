package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_LOCATIONS_TABLE = GameDataTableSpec(
	tableName = "game_location",
	label = "地点资料",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "region_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 地点资料 Service。
 */
@Service
class GameLocationsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_LOCATIONS_TABLE)
