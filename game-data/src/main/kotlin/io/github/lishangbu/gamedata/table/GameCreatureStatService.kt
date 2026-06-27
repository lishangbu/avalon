package io.github.lishangbu.gamedata.table

import org.springframework.stereotype.Service

private val GAME_CREATURE_STAT_TABLE = GameDataTableSpec(
	tableName = "game_creature_stat",
	label = "生物数值绑定",
	columns = listOf(
		GameDataColumnSpec(name = "creature_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "stat_id", type = GameDataColumnType.LONG, required = true),
		GameDataColumnSpec(name = "base_value", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "effort", type = GameDataColumnType.INT),
	),
	searchColumns = listOf("creature_id", "stat_id"),
)

/**
 * 生物数值绑定 Service。
 */
@Service
class GameCreatureStatService(
	operations: GameDataJdbcOperations,
) : GameDataTableService(operations, GAME_CREATURE_STAT_TABLE)
