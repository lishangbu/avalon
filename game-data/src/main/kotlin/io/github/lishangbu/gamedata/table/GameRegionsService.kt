package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_REGIONS_TABLE = GameDataTableSpec(
	tableName = "game_region",
	label = "地区资料",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "main_generation_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 地区资料 Service。
 */
@Service
class GameRegionsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_REGIONS_TABLE)
