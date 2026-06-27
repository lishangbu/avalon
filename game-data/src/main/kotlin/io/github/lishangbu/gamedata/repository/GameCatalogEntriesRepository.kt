package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_CATALOG_ENTRIES_TABLE = GameDataTableSpec(
	tableName = "game_catalog_entry",
	label = "图鉴目录条目",
	columns = listOf(
		GameDataColumnSpec(name = "catalog_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "species_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "entry_number", type = GameDataColumnType.INT, required = true),
	),
	searchColumns = listOf("catalog_id", "species_id", "entry_number"),
)

/**
 * 图鉴目录条目持久化访问。
 */
@Repository
class GameCatalogEntriesRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_CATALOG_ENTRIES_TABLE)
