package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_LOCATION_AREA_ENCOUNTER_CONDITION_VALUES_TABLE = GameDataTableSpec(
	tableName = "game_location_area_encounter_condition_value",
	label = "区域遭遇条件绑定",
	columns = listOf(
		GameDataColumnSpec(name = "encounter_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "condition_value_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("encounter_id", "condition_value_id"),
)

/**
 * 区域遭遇条件绑定 Service。
 */
@Service
class GameLocationAreaEncounterConditionValuesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_LOCATION_AREA_ENCOUNTER_CONDITION_VALUES_TABLE)
