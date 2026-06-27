package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_VERSIONS_TABLE = GameDataTableSpec(
	tableName = "game_version",
	label = "版本资料",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "version_group_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 版本资料 Service。
 */
@Service
class GameVersionsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_VERSIONS_TABLE)
