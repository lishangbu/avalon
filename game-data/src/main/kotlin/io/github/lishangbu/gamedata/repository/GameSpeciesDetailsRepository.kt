package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_SPECIES_DETAILS_TABLE = GameDataTableSpec(
	tableName = "game_species_detail",
	label = "种类详情",
	columns = listOf(
		GameDataColumnSpec(name = "species_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "growth_rate_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "evolves_from_species_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "evolution_chain_id", type = GameDataColumnType.LONG),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT),
		GameDataColumnSpec(name = "gender_differences", type = GameDataColumnType.BOOLEAN, required = true),
		GameDataColumnSpec(name = "forms_switchable", type = GameDataColumnType.BOOLEAN, required = true),
		GameDataColumnSpec(name = "genus", type = GameDataColumnType.STRING, maxLength = 200),
		GameDataColumnSpec(name = "flavor_text", type = GameDataColumnType.STRING),
	),
	searchColumns = listOf("species_id", "genus", "flavor_text"),
)

/**
 * 种类详情持久化访问。
 */
@Repository
class GameSpeciesDetailsRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_SPECIES_DETAILS_TABLE)
