package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

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
 * 图鉴目录条目 Service。
 */
@Service
class GameCatalogEntriesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_CATALOG_ENTRIES_TABLE)
