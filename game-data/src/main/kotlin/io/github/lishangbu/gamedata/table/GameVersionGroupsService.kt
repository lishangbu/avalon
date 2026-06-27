package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_VERSION_GROUPS_TABLE = GameDataTableSpec(
	tableName = "game_version_group",
	label = "版本组",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "generation_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 版本组 Service。
 */
@Service
class GameVersionGroupsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_VERSION_GROUPS_TABLE)
