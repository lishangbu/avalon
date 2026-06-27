package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_CATALOG_VERSION_GROUPS_TABLE = GameDataTableSpec(
	tableName = "game_catalog_version_group",
	label = "目录版本组绑定",
	columns = listOf(
		GameDataColumnSpec(name = "catalog_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "version_group_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("catalog_id", "version_group_id"),
)

/**
 * 目录版本组绑定 Service。
 */
@Service
class GameCatalogVersionGroupsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_CATALOG_VERSION_GROUPS_TABLE)
