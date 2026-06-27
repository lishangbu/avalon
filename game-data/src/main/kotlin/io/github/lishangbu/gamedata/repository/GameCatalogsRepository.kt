package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

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
 * 图鉴目录持久化访问。
 */
@Repository
class GameCatalogsRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_CATALOGS_TABLE)
