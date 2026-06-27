package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_SPECIES_TABLE = GameDataTableSpec(
	tableName = "game_species",
	label = "种类资料",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "color_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "shape_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "habitat_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "gender_rate", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "capture_rate", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "base_happiness", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "hatch_counter", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "baby", type = GameDataColumnType.BOOLEAN),
		GameDataColumnSpec(name = "legendary", type = GameDataColumnType.BOOLEAN),
		GameDataColumnSpec(name = "mythical", type = GameDataColumnType.BOOLEAN),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 种类资料 Service。
 */
@Service
class GameSpeciesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_SPECIES_TABLE)
