package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

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
 * 生物数值绑定持久化访问。
 */
@Repository
class GameCreatureStatRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_CREATURE_STAT_TABLE)
