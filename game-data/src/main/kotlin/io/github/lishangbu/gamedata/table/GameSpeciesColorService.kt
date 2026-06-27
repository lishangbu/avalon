package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_SPECIES_COLOR_TABLE = GameDataTableSpec(
	tableName = "game_species_color",
	label = "种类颜色",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 种类颜色 Service。
 */
@Service
class GameSpeciesColorService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_SPECIES_COLOR_TABLE)
