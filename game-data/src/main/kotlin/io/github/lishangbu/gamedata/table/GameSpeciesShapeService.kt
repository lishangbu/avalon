package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_SPECIES_SHAPE_TABLE = GameDataTableSpec(
	tableName = "game_species_shape",
	label = "种类形态",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 种类形态 Service。
 */
@Service
class GameSpeciesShapeService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_SPECIES_SHAPE_TABLE)
