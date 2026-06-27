package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_REGIONS_TABLE = GameDataTableSpec(
	tableName = "game_region",
	label = "地区资料",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 地区资料持久化访问。
 */
@Repository
class GameRegionsRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_REGIONS_TABLE)
