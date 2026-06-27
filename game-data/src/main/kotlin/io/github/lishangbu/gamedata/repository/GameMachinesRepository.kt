package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_MACHINES_TABLE = GameDataTableSpec(
	tableName = "game_machine",
	label = "机器资料",
	columns = listOf(
		GameDataColumnSpec(name = "item_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "skill_id", type = GameDataColumnType.LONG, required = true),
	),
	searchColumns = listOf("item_id", "skill_id"),
)

/**
 * 机器资料持久化访问。
 */
@Repository
class GameMachinesRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_MACHINES_TABLE)
