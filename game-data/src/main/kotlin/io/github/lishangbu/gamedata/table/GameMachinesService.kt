package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_MACHINES_TABLE = GameDataTableSpec(
	tableName = "game_machine",
	label = "机器资料",
	columns = listOf(
		GameDataColumnSpec(name = "item_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "skill_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "version_group_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("item_id", "skill_id", "version_group_id"),
)

/**
 * 机器资料 Service。
 */
@Service
class GameMachinesService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_MACHINES_TABLE)
