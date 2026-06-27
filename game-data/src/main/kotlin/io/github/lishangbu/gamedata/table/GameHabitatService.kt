package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_HABITAT_TABLE = GameDataTableSpec(
	tableName = "game_habitat",
	label = "栖息地",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 栖息地 Service。
 */
@Service
class GameHabitatService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_HABITAT_TABLE)
