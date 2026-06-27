package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_CATALOGS_TABLE = GameDataTableSpec(
	tableName = "game_catalog",
	label = "图鉴目录",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "region_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "main_series", type = GameDataColumnType.BOOLEAN, required = true),
		GameDataColumnSpec(name = "description", type = GameDataColumnType.STRING),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name", "description"),
)

/**
 * 图鉴目录 Service。
 */
@Service
class GameCatalogsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_CATALOGS_TABLE)
