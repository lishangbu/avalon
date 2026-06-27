package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_BERRY_FLAVORS_TABLE = GameDataTableSpec(
	tableName = "game_berry_flavor",
	label = "树果口味",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "contest_type_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 树果口味 Service。
 */
@Service
class GameBerryFlavorsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_BERRY_FLAVORS_TABLE)
