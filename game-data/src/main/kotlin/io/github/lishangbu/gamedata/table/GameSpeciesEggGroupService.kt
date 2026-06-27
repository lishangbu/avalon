package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_SPECIES_EGG_GROUP_TABLE = GameDataTableSpec(
	tableName = "game_species_egg_group",
	label = "种类分组绑定",
	columns = listOf(
		GameDataColumnSpec(name = "species_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "egg_group_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "slot_order", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("species_id", "egg_group_id"),
)

/**
 * 种类分组绑定 Service。
 */
@Service
class GameSpeciesEggGroupService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_SPECIES_EGG_GROUP_TABLE)
