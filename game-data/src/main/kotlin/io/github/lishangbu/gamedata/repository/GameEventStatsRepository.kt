package io.github.lishangbu.gamedata.repository

import org.springframework.stereotype.Repository

private val GAME_EVENT_STATS_TABLE = GameDataTableSpec(
	tableName = "game_event_stat",
	label = "活动能力项",
	columns = listOf(
		GameDataColumnSpec(name = "code", type = GameDataColumnType.STRING, required = true, maxLength = 80),
		GameDataColumnSpec(name = "name", type = GameDataColumnType.STRING, required = true, maxLength = 120),
		GameDataColumnSpec(name = "sort_order", type = GameDataColumnType.INT, required = true),
		GameDataColumnSpec(name = "enabled", type = GameDataColumnType.BOOLEAN, required = true),
	),
	searchColumns = listOf("code", "name"),
)

/**
 * 活动能力项持久化访问。
 */
@Repository
class GameEventStatsRepository(
	operations: GameDataJdbcOperations,
) : GameDataTableRepository(operations, GAME_EVENT_STATS_TABLE)
