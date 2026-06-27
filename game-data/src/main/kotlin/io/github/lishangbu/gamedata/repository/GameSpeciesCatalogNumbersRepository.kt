package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_SPECIES_CATALOG_NUMBERS_TABLE = GameDataTableSpec(
	tableName = "game_species_catalog_number",
	label = "种类目录编号",
	columns = listOf(
		GameDataColumnSpec(name = "species_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "catalog_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "entry_number", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("species_id", "catalog_id"),
)

/**
 * 种类目录编号持久化访问。
 */
@Repository
class GameSpeciesCatalogNumbersRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_SPECIES_CATALOG_NUMBERS_TABLE)
