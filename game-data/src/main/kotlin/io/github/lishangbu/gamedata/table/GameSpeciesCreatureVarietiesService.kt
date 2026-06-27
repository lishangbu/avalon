package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_SPECIES_CREATURE_VARIETIES_TABLE = GameDataTableSpec(
	tableName = "game_species_creature_variety",
	label = "种类生物变种",
	columns = listOf(
		GameDataColumnSpec(name = "species_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "default_variety", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("species_id", "creature_id"),
)

/**
 * 种类生物变种 Service。
 */
@Service
class GameSpeciesCreatureVarietiesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_SPECIES_CREATURE_VARIETIES_TABLE)
