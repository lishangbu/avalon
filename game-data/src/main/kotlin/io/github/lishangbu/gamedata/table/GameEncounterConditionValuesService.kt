package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_ENCOUNTER_CONDITION_VALUES_TABLE = GameDataTableSpec(
	tableName = "game_encounter_condition_value",
	label = "遭遇条件值",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 300),
		GameDataColumnSpec(name = "condition_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 遭遇条件值 Service。
 */
@Service
class GameEncounterConditionValuesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_ENCOUNTER_CONDITION_VALUES_TABLE)
