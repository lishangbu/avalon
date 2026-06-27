package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_ENCOUNTER_METHODS_TABLE = GameDataTableSpec(
	tableName = "game_encounter_method",
	label = "遭遇方式",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 300),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 遭遇方式 Service。
 */
@Service
class GameEncounterMethodsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_ENCOUNTER_METHODS_TABLE)
