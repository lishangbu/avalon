package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_ENCOUNTER_CONDITIONS_TABLE = GameDataTableSpec(
	tableName = "game_encounter_condition",
	label = "遭遇条件",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 遭遇条件持久化访问。
 */
@Repository
class GameEncounterConditionsRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_ENCOUNTER_CONDITIONS_TABLE)
