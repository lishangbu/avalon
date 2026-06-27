package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_BERRY_FIRMNESSES_TABLE = GameDataTableSpec(
	tableName = "game_berry_firmness",
	label = "树果硬度",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 树果硬度 Service。
 */
@Service
class GameBerryFirmnessesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_BERRY_FIRMNESSES_TABLE)
