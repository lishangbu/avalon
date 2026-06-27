package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_VERSION_GROUP_REGIONS_TABLE = GameDataTableSpec(
	tableName = "game_version_group_region",
	label = "版本组地区绑定",
	columns = listOf(
		GameDataColumnSpec(name = "version_group_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "region_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("version_group_id", "region_id"),
)

/**
 * 版本组地区绑定 Service。
 */
@Service
class GameVersionGroupRegionsService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_VERSION_GROUP_REGIONS_TABLE)
